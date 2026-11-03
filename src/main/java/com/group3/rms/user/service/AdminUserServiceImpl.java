package com.group3.rms.user.service;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.user.dto.AdminUserCreateRequest;
import com.group3.rms.user.dto.AdminUserDetailResponse;
import com.group3.rms.user.dto.AdminUserRoleUpdateRequest;
import com.group3.rms.user.dto.AdminUserSummaryResponse;
import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.mapper.AdminUserMapper;
import com.group3.rms.user.repository.AdminUserRepository;
import com.group3.rms.user.repository.RoleRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_KEYWORD_LENGTH = 255;

    private final AdminUserRepository adminUserRepository;
    private final AdminUserMapper adminUserMapper;
    private final CurrentUserService currentUserService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final Validator validator;

    @Override
    public Page<AdminUserSummaryResponse> searchUsers(String keyword, String role, String status, String page) {
        requireActiveAdmin();

        String keywordPattern = createKeywordPattern(keyword);
        RoleCode roleFilter = parseRole(role);
        UserStatus statusFilter = parseStatus(status);
        int pageNumber = parsePage(page);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, sort);

        Page<Long> userIds = adminUserRepository.searchUserIds(keywordPattern, roleFilter, statusFilter, pageable);
        if (pageNumber > 0 && pageNumber >= userIds.getTotalPages()) {
            throw new BusinessException("page", "Trang yêu cầu không tồn tại. Vui lòng quay về trang đầu.");
        }

        List<AdminUserSummaryResponse> responses = new ArrayList<>();
        if (userIds.hasContent()) {
            // Chỉ tải roles cho các ID của trang hiện tại, không fetch collection cùng Pageable.
            List<User> users = adminUserRepository.findAllWithRolesByIdIn(userIds.getContent());
            for (User user : users) {
                RoleCode userRole = getSingleRole(user);
                responses.add(adminUserMapper.toSummaryResponse(user, userRole));
            }
        }

        return new PageImpl<>(responses, pageable, userIds.getTotalElements());
    }

    @Override
    public void checkCreatePermission() {
        requireActiveAdmin();
    }

    @Override
    public void checkAccountAvailability(String username, String email) {
        requireActiveAdmin();

        if (adminUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("username", "Tên đăng nhập đã được sử dụng.");
        }
        if (adminUserRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("email", "Email đã được đăng ký.");
        }
    }

    @Override
    @Transactional
    public Long createUser(AdminUserCreateRequest request) {
        requireActiveAdmin();

        if (request == null) {
            throw new BusinessException("Vui lòng nhập thông tin tài khoản.");
        }
        if (!"HR".equals(request.role()) && !"INTERVIEWER".equals(request.role())) {
            throw new BusinessException("role", "Chỉ được tạo tài khoản HR hoặc INTERVIEWER.");
        }

        Set<ConstraintViolation<AdminUserCreateRequest>> violations = validator.validate(request);
        for (ConstraintViolation<AdminUserCreateRequest> violation : violations) {
            throw new BusinessException(violation.getPropertyPath().toString(), violation.getMessage());
        }

        checkAccountAvailability(request.username(), request.email());

        RoleCode roleCode = RoleCode.valueOf(request.role());
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("role", "Hệ thống chưa thiết lập vai trò " + roleCode + "."));

        User user = adminUserMapper.toUser(request);
        try {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        } catch (IllegalArgumentException e) {
            BusinessException error = new BusinessException(
                    "password", "Không thể mã hóa mật khẩu đã nhập. Vui lòng nhập mật khẩu khác."
            );
            error.initCause(e);
            throw error;
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role));

        // Flush để ràng buộc UNIQUE được kiểm tra trước khi ghi nhật ký.
        User savedUser = adminUserRepository.saveAndFlush(user);
        auditLogService.log(
                AuditAction.CREATE_ACCOUNT,
                TargetType.USER,
                savedUser.getId(),
                "Tạo tài khoản nhân viên: " + savedUser.getUsername() + ", vai trò: " + roleCode
        );

        return savedUser.getId();
    }

    @Override
    public AdminUserDetailResponse getUserDetail(Long id) {
        requireActiveAdmin();
        validateUserId(id);

        User user = adminUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản."));
        RoleCode role = getSingleRole(user);
        if (user.getStatus() == null) {
            throw new BusinessException("Dữ liệu trạng thái tài khoản không hợp lệ. Vui lòng kiểm tra lại dữ liệu.");
        }

        Long jobCount = null;
        if (role == RoleCode.HR) {
            jobCount = adminUserRepository.countJobsByOwnerId(id);
        }

        return adminUserMapper.toDetailResponse(user, role, jobCount);
    }

    @Override
    @Transactional
    public void lockUser(Long id, boolean confirmed) {
        requireActiveAdmin();
        User user = findUserForStatusChange(id);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Tài khoản đã ở trạng thái yêu cầu.");
        }
        if (!confirmed) {
            throw new BusinessException("Vui lòng xác nhận khóa tài khoản.");
        }

        user.setStatus(UserStatus.INACTIVE);
        adminUserRepository.saveAndFlush(user);
        auditLogService.log(
                AuditAction.LOCK_ACCOUNT,
                TargetType.USER,
                user.getId(),
                "Khóa tài khoản: " + user.getUsername()
        );
    }

    @Override
    @Transactional
    public void unlockUser(Long id, boolean confirmed) {
        requireActiveAdmin();
        User user = findUserForStatusChange(id);

        if (user.getStatus() != UserStatus.INACTIVE) {
            throw new BusinessException("Tài khoản đã ở trạng thái yêu cầu.");
        }
        if (!confirmed) {
            throw new BusinessException("Vui lòng xác nhận mở khóa tài khoản.");
        }

        user.setStatus(UserStatus.ACTIVE);
        adminUserRepository.saveAndFlush(user);
        auditLogService.log(
                AuditAction.UNLOCK_ACCOUNT,
                TargetType.USER,
                user.getId(),
                "Mở khóa tài khoản: " + user.getUsername()
        );
    }

    @Override
    @Transactional
    public boolean changeUserRole(Long id, AdminUserRoleUpdateRequest request) {
        requireActiveAdmin();
        validateUserId(id);

        // Đọc vai trò dưới khóa bản ghi để các yêu cầu đồng thời không cập nhật và ghi log trùng.
        User user = adminUserRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản."));
        RoleCode oldRole = getSingleRole(user);

        if ((oldRole != RoleCode.HR && oldRole != RoleCode.INTERVIEWER)
                || request == null
                || (!"HR".equals(request.role()) && !"INTERVIEWER".equals(request.role()))) {
            throw new BusinessException("role", "Chỉ được đổi vai trò giữa HR và INTERVIEWER.");
        }

        RoleCode newRole = RoleCode.valueOf(request.role());
        if (oldRole == newRole) {
            return false;
        }

        if (oldRole == RoleCode.HR) {
            long blockingJobs = adminUserRepository.countJobsBlockingRoleChange(
                    id,
                    List.of(JobStatus.DRAFT, JobStatus.ACTIVE),
                    JobStatus.CLOSED,
                    List.of(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW)
            );
            if (blockingJobs > 0) {
                throw new BusinessException("Tài khoản còn tin tuyển dụng hoặc hồ sơ đang xử lý, không thể đổi vai trò.");
            }
        }

        Role role = roleRepository.findByCode(newRole)
                .orElseThrow(() -> new BusinessException("role", "Hệ thống chưa thiết lập vai trò " + newRole + "."));

        user.getRoles().clear();
        user.getRoles().add(role);
        adminUserRepository.saveAndFlush(user);
        auditLogService.log(
                AuditAction.CHANGE_ROLE,
                TargetType.USER,
                user.getId(),
                "Đổi vai trò tài khoản " + user.getUsername() + ": " + oldRole + " -> " + newRole
        );

        return true;
    }

    private void validateUserId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("ID tài khoản phải là số nguyên lớn hơn 0.");
        }
    }

    private User findUserForStatusChange(Long id) {
        validateUserId(id);

        // Giữ khóa bản ghi đến hết giao dịch, tránh hai yêu cầu cùng ghi trạng thái và nhật ký.
        User user = adminUserRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản."));
        RoleCode role = getSingleRole(user);
        if (role == RoleCode.ADMIN) {
            throw new BusinessException("Không được thay đổi trạng thái tài khoản ADMIN.");
        }
        if (user.getStatus() == null) {
            throw new BusinessException("Dữ liệu trạng thái tài khoản không hợp lệ. Vui lòng kiểm tra lại dữ liệu.");
        }

        return user;
    }

    private void requireActiveAdmin() {
        CurrentUser current = currentUserService.require();
        if (current.id() == null || current.id() <= 0) {
            throw new ForbiddenException("Bạn không có quyền xem danh sách tài khoản.");
        }

        User actor = adminUserRepository.findById(current.id())
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền xem danh sách tài khoản."));

        Set<Role> roles = actor.getRoles();
        if (actor.getStatus() != UserStatus.ACTIVE || roles == null || roles.size() != 1) {
            throw new ForbiddenException("Chỉ quản trị viên đang hoạt động được xem danh sách tài khoản.");
        }

        Role role = roles.iterator().next();
        if (role == null || role.getCode() != RoleCode.ADMIN) {
            throw new ForbiddenException("Bạn không có quyền xem danh sách tài khoản.");
        }
    }

    private String createKeywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String value = keyword.trim();
        if (value.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException("keyword", "Từ khóa không được vượt quá 255 ký tự.");
        }

        // SQL Server còn dùng dấu [ để mở nhóm ký tự trong LIKE.
        value = value.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
        return "%" + value + "%";
    }

    private RoleCode parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        try {
            return RoleCode.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("role", "Vai trò lọc không hợp lệ. Vui lòng chọn lại vai trò.");
        }
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("status", "Trạng thái lọc không hợp lệ. Vui lòng chọn lại trạng thái.");
        }
    }

    private int parsePage(String page) {
        if (page == null || page.isBlank()) {
            return 0;
        }

        try {
            int pageNumber = Integer.parseInt(page.trim());
            // JPA nhận offset kiểu int, nên cần kiểm tra cả pageNumber * PAGE_SIZE.
            if (pageNumber < 0 || pageNumber > Integer.MAX_VALUE / PAGE_SIZE) {
                throw new BusinessException("page", "Số trang không hợp lệ. Vui lòng quay về trang đầu.");
            }
            return pageNumber;
        } catch (NumberFormatException e) {
            throw new BusinessException("page", "Số trang không hợp lệ. Vui lòng quay về trang đầu.");
        }
    }

    private RoleCode getSingleRole(User user) {
        Set<Role> roles = user.getRoles();
        if (roles == null || roles.size() != 1) {
            throw new BusinessException("Dữ liệu vai trò tài khoản không hợp lệ. Vui lòng kiểm tra lại dữ liệu.");
        }

        Role role = roles.iterator().next();
        if (role == null || role.getCode() == null) {
            throw new BusinessException("Dữ liệu vai trò tài khoản không hợp lệ. Vui lòng kiểm tra lại dữ liệu.");
        }
        return role.getCode();
    }
}
