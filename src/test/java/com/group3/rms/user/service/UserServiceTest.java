package com.group3.rms.user.service;

import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.LoginRequest;
import com.group3.rms.user.dto.PasswordChangeRequest;
import com.group3.rms.user.dto.RegisterRequest;
import com.group3.rms.user.dto.UserResponse;
import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.mapper.UserMapper;
import com.group3.rms.user.repository.RoleRepository;
import com.group3.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    private UserRepository fakeUserRepository;
    private RoleRepository fakeRoleRepository;
    private PasswordEncoder fakePasswordEncoder;
    private UserMapper fakeUserMapper;
    private FakeCurrentUserService fakeCurrentUserService;
    private FakeAuditLogService fakeAuditLogService;
    private UserServiceImpl userService;

    private User existingUser;
    private Role candidateRole;

    static class FakeCurrentUserService implements CurrentUserService {
        CurrentUser current;

        @Override
        public Optional<CurrentUser> get() {
            return Optional.ofNullable(current);
        }

        @Override
        public CurrentUser require() {
            return get().orElseThrow(() -> new RuntimeException("No user"));
        }

        @Override
        public void set(CurrentUser currentUser) {
            this.current = currentUser;
        }

        @Override
        public void clear() {
            this.current = null;
        }
    }

    static class FakeAuditLogService implements AuditLogService {
        AuditAction lastAction;
        TargetType lastTargetType;
        Long lastTargetId;
        String lastDetail;

        @Override
        public void log(AuditAction action, TargetType targetType, Long targetId, String detail) {
            this.lastAction = action;
            this.lastTargetType = targetType;
            this.lastTargetId = targetId;
            this.lastDetail = detail;
        }
    }

    @BeforeEach
    void setUp() {
        existingUser = new User();
        ReflectionTestUtils.setField(existingUser, "id", 10L);
        existingUser.setUsername("hr1");
        existingUser.setEmail("hr1@rms.local");
        existingUser.setFullName("Trần Thu Hà");
        existingUser.setPasswordHash("hashed_Test@123");
        existingUser.setStatus(UserStatus.ACTIVE);

        Role hrRole = new Role();
        hrRole.setId(2L);
        hrRole.setCode(RoleCode.HR);
        hrRole.setName("Nhân viên tuyển dụng");
        existingUser.setRoles(Set.of(hrRole));

        candidateRole = new Role();
        candidateRole.setId(4L);
        candidateRole.setCode(RoleCode.CANDIDATE);
        candidateRole.setName("Ứng viên");

        fakeUserRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByUsernameIgnoreCaseOrEmailIgnoreCase".equals(name)) {
                        String id1 = (String) args[0];
                        String id2 = (String) args[1];
                        if ("hr1".equalsIgnoreCase(id1) || "hr1@rms.local".equalsIgnoreCase(id2)) {
                            return Optional.of(existingUser);
                        }
                        return Optional.empty();
                    }
                    if ("existsByUsername".equals(name)) {
                        String username = (String) args[0];
                        return "hr1".equalsIgnoreCase(username) || "candidate1".equalsIgnoreCase(username);
                    }
                    if ("existsByEmail".equals(name)) {
                        String email = (String) args[0];
                        return "hr1@rms.local".equalsIgnoreCase(email) || "candidate1@example.com".equalsIgnoreCase(email);
                    }
                    if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        if (Long.valueOf(10L).equals(id)) {
                            return Optional.of(existingUser);
                        }
                        return Optional.empty();
                    }
                    if ("save".equals(name)) {
                        User userToSave = (User) args[0];
                        if (userToSave.getId() == null) {
                            ReflectionTestUtils.setField(userToSave, "id", 100L);
                        }
                        return userToSave;
                    }
                    return null;
                }
        );

        fakeRoleRepository = (RoleRepository) Proxy.newProxyInstance(
                RoleRepository.class.getClassLoader(),
                new Class<?>[]{RoleRepository.class},
                (proxy, method, args) -> {
                    if ("findByCode".equals(method.getName())) {
                        RoleCode code = (RoleCode) args[0];
                        if (code == RoleCode.CANDIDATE) {
                            return Optional.of(candidateRole);
                        }
                    }
                    return Optional.empty();
                }
        );

        fakePasswordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "hashed_" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return ("hashed_" + rawPassword).equals(encodedPassword);
            }
        };

        fakeUserMapper = user -> new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRoles().stream().findFirst().map(Role::getCode).orElse(null),
                user.getStatus()
        );

        fakeCurrentUserService = new FakeCurrentUserService();
        fakeAuditLogService = new FakeAuditLogService();

        userService = new UserServiceImpl(
                fakeUserRepository,
                fakeRoleRepository,
                fakePasswordEncoder,
                fakeUserMapper,
                fakeCurrentUserService,
                fakeAuditLogService
        );
    }

    /* =========================================================================
     * UC-01: LOGIN TESTS
     * ========================================================================= */

    @Test
    @DisplayName("UC-01: Đăng nhập thành công với username và mật khẩu đúng")
    void login_Success_ShouldReturnUserResponseAndSetCurrentUser() {
        LoginRequest request = new LoginRequest("hr1", "Test@123");

        UserResponse response = userService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.username()).isEqualTo("hr1");
        assertThat(response.role()).isEqualTo(RoleCode.HR);

        assertThat(fakeCurrentUserService.current).isNotNull();
        assertThat(fakeCurrentUserService.current.id()).isEqualTo(10L);
        assertThat(fakeCurrentUserService.current.username()).isEqualTo("hr1");
        assertThat(fakeCurrentUserService.current.role()).isEqualTo(RoleCode.HR);
    }

    @Test
    @DisplayName("UC-01: Đăng nhập thành công với email và mật khẩu đúng")
    void login_SuccessWithEmail_ShouldReturnUserResponse() {
        LoginRequest request = new LoginRequest("hr1@rms.local", "Test@123");

        UserResponse response = userService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("hr1");
        assertThat(fakeCurrentUserService.current).isNotNull();
    }

    @Test
    @DisplayName("UC-01: Đăng nhập thất bại do tài khoản không tồn tại")
    void login_UserNotFound_ShouldThrowBusinessException() {
        LoginRequest request = new LoginRequest("non_existent_user", "Test@123");

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tên đăng nhập hoặc mật khẩu không đúng.");

        assertThat(fakeCurrentUserService.current).isNull();
    }

    @Test
    @DisplayName("UC-01: Đăng nhập thất bại do sai mật khẩu")
    void login_WrongPassword_ShouldThrowBusinessException() {
        LoginRequest request = new LoginRequest("hr1", "WrongPassword");

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tên đăng nhập hoặc mật khẩu không đúng.");

        assertThat(fakeCurrentUserService.current).isNull();
    }

    @Test
    @DisplayName("UC-01: Đăng nhập thất bại do tài khoản bị khóa (INACTIVE)")
    void login_InactiveAccount_ShouldThrowBusinessException() {
        existingUser.setStatus(UserStatus.INACTIVE);
        LoginRequest request = new LoginRequest("hr1", "Test@123");

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");

        assertThat(fakeCurrentUserService.current).isNull();
    }

    /* =========================================================================
     * UC-02: REGISTER CANDIDATE TESTS
     * ========================================================================= */

    @Test
    @DisplayName("UC-02: Đăng ký thành công tài khoản Candidate mới")
    void register_Success_ShouldCreateCandidateUserAndLogAudit() {
        RegisterRequest request = new RegisterRequest(
                "Nguyễn Văn Mới",
                "candidate_new",
                "new_candidate@example.com",
                "Password@123",
                "Password@123"
        );

        UserResponse response = userService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.username()).isEqualTo("candidate_new");
        assertThat(response.email()).isEqualTo("new_candidate@example.com");
        assertThat(response.fullName()).isEqualTo("Nguyễn Văn Mới");
        assertThat(response.role()).isEqualTo(RoleCode.CANDIDATE);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);

        // Kiểm tra đã ghi audit log
        assertThat(fakeAuditLogService.lastAction).isEqualTo(AuditAction.REGISTER);
        assertThat(fakeAuditLogService.lastTargetType).isEqualTo(TargetType.USER);
        assertThat(fakeAuditLogService.lastTargetId).isEqualTo(100L);
    }

    @Test
    @DisplayName("UC-02: Đăng ký thất bại do tên đăng nhập đã được sử dụng")
    void register_DuplicateUsername_ShouldThrowBusinessException() {
        RegisterRequest request = new RegisterRequest(
                "Người Dùng Mới",
                "candidate1", // đã tồn tại
                "unique_email@example.com",
                "Password@123",
                "Password@123"
        );

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tên đăng nhập đã được sử dụng.")
                .extracting(e -> ((BusinessException) e).getField())
                .isEqualTo("username");
    }

    @Test
    @DisplayName("UC-02: Đăng ký thất bại do email đã được sử dụng")
    void register_DuplicateEmail_ShouldThrowBusinessException() {
        RegisterRequest request = new RegisterRequest(
                "Người Dùng Mới",
                "unique_user",
                "candidate1@example.com", // đã tồn tại
                "Password@123",
                "Password@123"
        );

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email đã được sử dụng.")
                .extracting(e -> ((BusinessException) e).getField())
                .isEqualTo("email");
    }

    @Test
    @DisplayName("UC-02: Đăng ký thất bại do mật khẩu xác nhận không khớp")
    void register_PasswordMismatch_ShouldThrowBusinessException() {
        RegisterRequest request = new RegisterRequest(
                "Người Dùng Mới",
                "unique_user",
                "unique_email@example.com",
                "Password@123",
                "DifferentPassword@123"
        );

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu xác nhận không khớp.")
                .extracting(e -> ((BusinessException) e).getField())
                .isEqualTo("confirmPassword");
    }

    /* =========================================================================
     * UC-04: CHANGE PASSWORD TESTS
     * ========================================================================= */

    @Test
    @DisplayName("UC-04: Đổi mật khẩu thành công khi thông tin hợp lệ")
    void changePassword_Success_ShouldUpdatePasswordHash() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        PasswordChangeRequest request = new PasswordChangeRequest(
                "Test@123", "NewSecret@456", "NewSecret@456"
        );

        userService.changePassword(request);

        assertThat(existingUser.getPasswordHash()).isEqualTo("hashed_NewSecret@456");
    }

    @Test
    @DisplayName("UC-04: Đổi mật khẩu thất bại do mật khẩu hiện tại không đúng")
    void changePassword_WrongCurrentPassword_ShouldThrowBusinessException() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        PasswordChangeRequest request = new PasswordChangeRequest(
                "WrongOldPassword", "NewSecret@456", "NewSecret@456"
        );

        assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu hiện tại không đúng.")
                .extracting(e -> ((BusinessException) e).getField())
                .isEqualTo("currentPassword");
    }

    @Test
    @DisplayName("UC-04: Đổi mật khẩu thất bại do mật khẩu mới trùng mật khẩu cũ")
    void changePassword_NewPasswordSameAsOld_ShouldThrowBusinessException() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        PasswordChangeRequest request = new PasswordChangeRequest(
                "Test@123", "Test@123", "Test@123"
        );

        assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu mới không được trùng với mật khẩu hiện tại.")
                .extracting(e -> ((BusinessException) e).getField())
                .isEqualTo("newPassword");
    }

    @Test
    @DisplayName("UC-04: Đổi mật khẩu thất bại do xác nhận mật khẩu không khớp")
    void changePassword_PasswordMismatch_ShouldThrowBusinessException() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        PasswordChangeRequest request = new PasswordChangeRequest(
                "Test@123", "NewSecret@456", "DifferentSecret@789"
        );

        assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu xác nhận không khớp.")
                .extracting(e -> ((BusinessException) e).getField())
                .isEqualTo("confirmPassword");
    }
}
