package com.group3.rms.user.service;

import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.LoginRequest;
import com.group3.rms.user.dto.PasswordChangeRequest;
import com.group3.rms.user.dto.ProfileUpdateRequest;
import com.group3.rms.user.dto.RegisterRequest;
import com.group3.rms.user.dto.UserResponse;
import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.mapper.UserMapper;
import com.group3.rms.user.repository.RoleRepository;
import com.group3.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        log.info("Xử lý đăng nhập cho: {}", identifier);

        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> {
                    log.warn("Đăng nhập thất bại: Không tìm thấy tài khoản '{}'", identifier);
                    return new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng.");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Đăng nhập thất bại cho '{}': Mật khẩu không chính xác", identifier);
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Đăng nhập bị từ chối cho '{}': Tài khoản INACTIVE", identifier);
            throw new BusinessException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        RoleCode primaryRole = user.getRoles().stream()
                .findFirst()
                .map(Role::getCode)
                .orElse(RoleCode.CANDIDATE);

        CurrentUser currentUser = new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                primaryRole
        );
        currentUserService.set(currentUser);

        log.info("Người dùng '{}' đăng nhập thành công với vai trò {}", user.getUsername(), primaryRole);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        log.info("Xử lý đăng ký tài khoản Candidate: username='{}', email='{}'", username, email);

        if (userRepository.existsByUsername(username)) {
            log.warn("Đăng ký thất bại: Tên đăng nhập '{}' đã tồn tại", username);
            throw new BusinessException("username", "Tên đăng nhập đã được sử dụng.");
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("Đăng ký thất bại: Email '{}' đã tồn tại", email);
            throw new BusinessException("email", "Email đã được sử dụng.");
        }

        if (!request.password().equals(request.confirmPassword())) {
            log.warn("Đăng ký thất bại cho '{}': Mật khẩu xác nhận không khớp", username);
            throw new BusinessException("confirmPassword", "Mật khẩu xác nhận không khớp.");
        }

        Role candidateRole = roleRepository.findByCode(RoleCode.CANDIDATE)
                .orElseThrow(() -> new BusinessException("Hệ thống chưa thiết lập vai trò CANDIDATE."));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(candidateRole));

        User savedUser = userRepository.save(user);

        auditLogService.log(
                savedUser.getUsername(),
                AuditAction.REGISTER,
                TargetType.USER,
                savedUser.getId(),
                "Ứng viên tự đăng ký tài khoản: " + savedUser.getUsername()
        );

        log.info("Tài khoản Candidate '{}' (ID={}) đã được tạo thành công", savedUser.getUsername(), savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        CurrentUser current = currentUserService.require();
        log.info("Xử lý đổi mật khẩu cho user: '{}'", current.username());

        User user = userRepository.findById(current.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin tài khoản."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Đổi mật khẩu thất bại cho '{}': Mật khẩu hiện tại không đúng", current.username());
            throw new BusinessException("currentPassword", "Mật khẩu hiện tại không đúng.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            log.warn("Đổi mật khẩu thất bại cho '{}': Mật khẩu mới trùng mật khẩu cũ", current.username());
            throw new BusinessException("newPassword", "Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            log.warn("Đổi mật khẩu thất bại cho '{}': Mật khẩu xác nhận không khớp", current.username());
            throw new BusinessException("confirmPassword", "Mật khẩu xác nhận không khớp.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Người dùng '{}' (ID={}) đã đổi mật khẩu thành công", user.getUsername(), user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        CurrentUser current = currentUserService.require();
        User user = userRepository.findById(current.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin tài khoản."));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(ProfileUpdateRequest request) {
        CurrentUser current = currentUserService.require();
        log.info("Xử lý cập nhật thông tin cá nhân cho user: '{}'", current.username());

        User user = userRepository.findById(current.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin tài khoản."));

        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone() != null && !request.phone().isBlank() ? request.phone().trim() : null);

        User savedUser = userRepository.save(user);

        // Cập nhật lại phiên người dùng hiện tại
        currentUserService.set(new CurrentUser(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getFullName(),
                current.role()
        ));

        log.info("Người dùng '{}' đã cập nhật thông tin cá nhân thành công", savedUser.getUsername());
        return userMapper.toResponse(savedUser);
    }
}
