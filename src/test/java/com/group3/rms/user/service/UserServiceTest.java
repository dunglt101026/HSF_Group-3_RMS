package com.group3.rms.user.service;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.LoginRequest;
import com.group3.rms.user.dto.UserResponse;
import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.mapper.UserMapper;
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
    private PasswordEncoder fakePasswordEncoder;
    private UserMapper fakeUserMapper;
    private FakeCurrentUserService fakeCurrentUserService;
    private UserServiceImpl userService;

    private User existingUser;

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

        fakeUserRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findByUsernameIgnoreCaseOrEmailIgnoreCase".equals(method.getName())) {
                        String id1 = (String) args[0];
                        String id2 = (String) args[1];
                        if ("hr1".equalsIgnoreCase(id1) || "hr1@rms.local".equalsIgnoreCase(id2)) {
                            return Optional.of(existingUser);
                        }
                        return Optional.empty();
                    }
                    return null;
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

        userService = new UserServiceImpl(
                fakeUserRepository,
                fakePasswordEncoder,
                fakeUserMapper,
                fakeCurrentUserService
        );
    }

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
}
