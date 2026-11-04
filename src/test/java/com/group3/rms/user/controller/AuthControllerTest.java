package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.RegisterRequest;
import com.group3.rms.user.dto.UserResponse;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests cho AuthController sau Sprint 3.
 * - POST /login xử lý bởi Spring Security → không test trong controller unit test.
 * - GET /logout & POST /logout xử lý bởi Spring Security → không test trong controller unit test.
 */
class AuthControllerTest {

    private AuthController authController;
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
        UserResponse responseToReturn;
        BusinessException exceptionToThrow;

        @Override
        public UserResponse login(com.group3.rms.user.dto.LoginRequest request) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return responseToReturn;
        }

        @Override
        public UserResponse register(RegisterRequest request) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return responseToReturn;
        }

        @Override
        public void changePassword(com.group3.rms.user.dto.PasswordChangeRequest request) {
        }

        @Override
        public UserResponse getCurrentUserProfile() {
            return responseToReturn;
        }

        @Override
        public UserResponse updateProfile(com.group3.rms.user.dto.ProfileUpdateRequest request) {
            return responseToReturn;
        }
    }

    @BeforeEach
    void setUp() {
        fakeCurrentUserService = new FakeCurrentUserService();
        fakeUserService = new FakeUserService();
        authController = new AuthController(fakeUserService, fakeCurrentUserService);
    }

    /* =========================================================================
     * UC-01: LOGIN TESTS (GET only — POST handled by Spring Security)
     * ========================================================================= */

    @Test
    @DisplayName("GET /login: Chưa đăng nhập -> Trả về view auth/login")
    void getLogin_WhenNotLoggedIn_ShouldReturnLoginPage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();

        String view = authController.loginPage(request, model);

        assertThat(view).isEqualTo("auth/login");
    }

    @Test
    @DisplayName("GET /login: Đã đăng nhập vai trò ADMIN -> Tự động chuyển hướng về /admin/users")
    void getLogin_WhenAlreadyLoggedInAsAdmin_ShouldRedirectToAdminHome() {
        fakeCurrentUserService.current = new CurrentUser(1L, "admin", "Admin", RoleCode.ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();

        String view = authController.loginPage(request, model);

        assertThat(view).isEqualTo("redirect:/admin/users");
    }

    @Test
    @DisplayName("GET /login: Đã đăng nhập vai trò HR -> Tự động chuyển hướng về /hr/jobs")
    void getLogin_WhenAlreadyLoggedInAsHr_ShouldRedirectToHrHome() {
        fakeCurrentUserService.current = new CurrentUser(2L, "hr1", "HR 1", RoleCode.HR);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();

        String view = authController.loginPage(request, model);

        assertThat(view).isEqualTo("redirect:/hr/jobs");
    }

    @Test
    @DisplayName("GET /login: Đã đăng nhập vai trò INTERVIEWER -> Tự động chuyển hướng về /interviews/my")
    void getLogin_WhenAlreadyLoggedInAsInterviewer_ShouldRedirectToInterviewerHome() {
        fakeCurrentUserService.current = new CurrentUser(4L, "interviewer1", "Interviewer 1", RoleCode.INTERVIEWER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();

        String view = authController.loginPage(request, model);

        assertThat(view).isEqualTo("redirect:/interviews/my");
    }

    @Test
    @DisplayName("GET /login: Đã đăng nhập vai trò CANDIDATE -> Tự động chuyển hướng về /")
    void getLogin_WhenAlreadyLoggedInAsCandidate_ShouldRedirectToCandidateHome() {
        fakeCurrentUserService.current = new CurrentUser(6L, "candidate1", "Candidate 1", RoleCode.CANDIDATE);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();

        String view = authController.loginPage(request, model);

        assertThat(view).isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("GET /login?error: Có loginError trong session -> Hiển thị errorMessage và xóa khỏi session")
    void getLogin_WithLoginError_ShouldShowErrorMessage() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginError", "Tên đăng nhập hoặc mật khẩu không đúng.");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.addParameter("error", "");
        Model model = new ConcurrentModel();

        String view = authController.loginPage(request, model);

        assertThat(view).isEqualTo("auth/login");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Tên đăng nhập hoặc mật khẩu không đúng.");
        assertThat(session.getAttribute("loginError")).isNull();
    }

    @Test
    @DisplayName("GET /login?logout: Hiển thị successMessage đăng xuất thành công")
    void getLogin_WithLogoutParam_ShouldShowSuccessMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("logout", "");
        Model model = new ConcurrentModel();

        String view = authController.loginPage(request, model);

        assertThat(view).isEqualTo("auth/login");
        assertThat(model.getAttribute("successMessage")).isEqualTo("Bạn đã đăng xuất thành công.");
    }

    /* =========================================================================
     * UC-02: REGISTER CANDIDATE TESTS
     * ========================================================================= */

    @Test
    @DisplayName("GET /register: Chưa đăng nhập -> Trả về view auth/register và form rỗng")
    void getRegister_WhenNotLoggedIn_ShouldReturnRegisterPage() {
        Model model = new ConcurrentModel();

        String view = authController.registerPage(model);

        assertThat(view).isEqualTo("auth/register");
        assertThat(model.getAttribute("form")).isNotNull();
    }

    @Test
    @DisplayName("GET /register: Đã đăng nhập vai trò CANDIDATE -> Tự động chuyển hướng về /")
    void getRegister_WhenAlreadyLoggedIn_ShouldRedirectToHome() {
        fakeCurrentUserService.current = new CurrentUser(6L, "candidate1", "Candidate 1", RoleCode.CANDIDATE);
        Model model = new ConcurrentModel();

        String view = authController.registerPage(model);

        assertThat(view).isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("POST /register: Form có lỗi validation -> Trả về view auth/register")
    void postRegister_ValidationErrors_ShouldReturnRegisterPage() {
        RegisterRequest form = new RegisterRequest("", "", "", "", "");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("username", "NotBlank", "Vui lòng nhập tên đăng nhập");

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = authController.register(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("auth/register");
    }

    @Test
    @DisplayName("POST /register: Đăng ký thành công -> Flash success và chuyển hướng về /login")
    void postRegister_Success_ShouldRedirectToLogin() {
        RegisterRequest form = new RegisterRequest(
                "Nguyễn Văn An", "candidate_new", "new@example.com", "Password@123", "Password@123"
        );
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        fakeUserService.responseToReturn = new UserResponse(
                50L, "candidate_new", "new@example.com", "Nguyễn Văn An", null, RoleCode.CANDIDATE, UserStatus.ACTIVE
        );

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = authController.register(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("redirect:/login");
        assertThat(ra.getFlashAttributes().get("success")).isEqualTo("Tạo tài khoản thành công, vui lòng đăng nhập.");
    }

    @Test
    @DisplayName("POST /register: Tên đăng nhập trùng -> Gắn lỗi vào bindingResult và ở lại trang register")
    void postRegister_DuplicateUsername_ShouldBindErrorAndReturnRegisterPage() {
        RegisterRequest form = new RegisterRequest(
                "Nguyễn Văn An", "candidate1", "new@example.com", "Password@123", "Password@123"
        );
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        fakeUserService.exceptionToThrow = new BusinessException("username", "Tên đăng nhập đã được sử dụng.");

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = authController.register(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("auth/register");
        assertThat(bindingResult.hasFieldErrors("username")).isTrue();
        assertThat(bindingResult.getFieldError("username").getDefaultMessage()).isEqualTo("Tên đăng nhập đã được sử dụng.");
    }
}
