package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.LoginRequest;
import com.group3.rms.user.dto.PasswordChangeRequest;
import com.group3.rms.user.dto.RegisterRequest;
import com.group3.rms.user.dto.UserResponse;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileControllerTest {

    private ProfileController profileController;
    private FakeCurrentUserService fakeCurrentUserService;
    private FakeUserService fakeUserService;

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

    static class FakeUserService implements UserService {
        UserResponse profileToReturn;
        BusinessException exceptionToThrow;
        PasswordChangeRequest lastChangePasswordRequest;

        @Override
        public UserResponse login(LoginRequest request) {
            return null;
        }

        @Override
        public UserResponse register(RegisterRequest request) {
            return null;
        }

        @Override
        public void changePassword(PasswordChangeRequest request) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            this.lastChangePasswordRequest = request;
        }

        @Override
        public UserResponse getCurrentUserProfile() {
            return profileToReturn;
        }
    }

    @BeforeEach
    void setUp() {
        fakeCurrentUserService = new FakeCurrentUserService();
        fakeUserService = new FakeUserService();
        profileController = new ProfileController(fakeUserService, fakeCurrentUserService);
    }

    @Test
    @DisplayName("GET /profile: Chưa đăng nhập -> Chuyển hướng về /login")
    void viewProfile_WhenNotLoggedIn_ShouldRedirectToLogin() {
        Model model = new ConcurrentModel();

        String view = profileController.viewProfile(model);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    @DisplayName("GET /profile: Đã đăng nhập -> Trả về view profile/view kèm thông tin user")
    void viewProfile_WhenLoggedIn_ShouldReturnProfileView() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        fakeUserService.profileToReturn = new UserResponse(
                10L, "hr1", "hr1@rms.local", "Trần Thu Hà", "0901000001", RoleCode.HR, UserStatus.ACTIVE
        );
        Model model = new ConcurrentModel();

        String view = profileController.viewProfile(model);

        assertThat(view).isEqualTo("profile/view");
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    @DisplayName("GET /profile/password: Chưa đăng nhập -> Chuyển hướng về /login")
    void passwordPage_WhenNotLoggedIn_ShouldRedirectToLogin() {
        Model model = new ConcurrentModel();

        String view = profileController.passwordPage(model);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    @DisplayName("GET /profile/password: Đã đăng nhập -> Trả về view profile/password")
    void passwordPage_WhenLoggedIn_ShouldReturnPasswordPage() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        Model model = new ConcurrentModel();

        String view = profileController.passwordPage(model);

        assertThat(view).isEqualTo("profile/password");
        assertThat(model.getAttribute("form")).isNotNull();
    }

    @Test
    @DisplayName("POST /profile/password: Form có lỗi validation -> Trả về profile/password")
    void postPassword_ValidationErrors_ShouldReturnPasswordPage() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        PasswordChangeRequest form = new PasswordChangeRequest("", "", "");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("currentPassword", "NotBlank", "Vui lòng nhập mật khẩu hiện tại");

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = profileController.changePassword(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("profile/password");
    }

    @Test
    @DisplayName("POST /profile/password: Đổi mật khẩu thành công -> Chuyển hướng về /profile kèm flash success")
    void postPassword_Success_ShouldRedirectToProfile() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        PasswordChangeRequest form = new PasswordChangeRequest("OldPass@123", "NewPass@456", "NewPass@456");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = profileController.changePassword(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("redirect:/profile");
        assertThat(ra.getFlashAttributes().get("success")).isEqualTo("Đổi mật khẩu thành công.");
        assertThat(fakeUserService.lastChangePasswordRequest).isNotNull();
    }

    @Test
    @DisplayName("POST /profile/password: Sai mật khẩu cũ -> Gắn lỗi vào bindingResult và trả về profile/password")
    void postPassword_WrongCurrentPassword_ShouldBindErrorAndReturnPasswordPage() {
        fakeCurrentUserService.current = new CurrentUser(10L, "hr1", "Trần Thu Hà", RoleCode.HR);
        PasswordChangeRequest form = new PasswordChangeRequest("WrongPass@123", "NewPass@456", "NewPass@456");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        fakeUserService.exceptionToThrow = new BusinessException("currentPassword", "Mật khẩu hiện tại không đúng.");

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = profileController.changePassword(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("profile/password");
        assertThat(bindingResult.hasFieldErrors("currentPassword")).isTrue();
        assertThat(bindingResult.getFieldError("currentPassword").getDefaultMessage()).isEqualTo("Mật khẩu hiện tại không đúng.");
    }
}
