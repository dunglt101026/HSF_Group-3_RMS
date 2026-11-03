package com.group3.rms.user.service;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.AdminUserSummaryResponse;
import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.mapper.AdminUserMapper;
import com.group3.rms.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
