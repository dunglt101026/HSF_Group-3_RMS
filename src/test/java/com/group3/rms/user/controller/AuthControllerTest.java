package com.group3.rms.user.controller;

import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.dto.LoginRequest;
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
        public UserResponse login(LoginRequest request) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return responseToReturn;
        }
    }

    @BeforeEach
    void setUp() {
        fakeCurrentUserService = new FakeCurrentUserService();
        fakeUserService = new FakeUserService();
        authController = new AuthController(fakeUserService, fakeCurrentUserService);
    }

    @Test
    @DisplayName("GET /login: Chưa đăng nhập -> Trả về view auth/login và form rỗng")
    void getLogin_WhenNotLoggedIn_ShouldReturnLoginPage() {
        Model model = new ConcurrentModel();

        String view = authController.loginPage(model);

        assertThat(view).isEqualTo("auth/login");
        assertThat(model.getAttribute("form")).isNotNull();
    }

    @Test
    @DisplayName("GET /login: Đã đăng nhập vai trò ADMIN -> Tự động chuyển hướng về /admin/users")
    void getLogin_WhenAlreadyLoggedInAsAdmin_ShouldRedirectToAdminHome() {
        fakeCurrentUserService.current = new CurrentUser(1L, "admin", "Admin", RoleCode.ADMIN);
        Model model = new ConcurrentModel();

        String view = authController.loginPage(model);

        assertThat(view).isEqualTo("redirect:/admin/users");
    }

    @Test
    @DisplayName("GET /login: Đã đăng nhập vai trò HR -> Tự động chuyển hướng về /hr/jobs")
    void getLogin_WhenAlreadyLoggedInAsHr_ShouldRedirectToHrHome() {
        fakeCurrentUserService.current = new CurrentUser(2L, "hr1", "HR 1", RoleCode.HR);
        Model model = new ConcurrentModel();

        String view = authController.loginPage(model);

        assertThat(view).isEqualTo("redirect:/hr/jobs");
    }

    @Test
    @DisplayName("GET /login: Đã đăng nhập vai trò INTERVIEWER -> Tự động chuyển hướng về /interviews/my")
    void getLogin_WhenAlreadyLoggedInAsInterviewer_ShouldRedirectToInterviewerHome() {
        fakeCurrentUserService.current = new CurrentUser(4L, "interviewer1", "Interviewer 1", RoleCode.INTERVIEWER);
        Model model = new ConcurrentModel();

        String view = authController.loginPage(model);

        assertThat(view).isEqualTo("redirect:/interviews/my");
    }

    @Test
    @DisplayName("GET /login: Đã đăng nhập vai trò CANDIDATE -> Tự động chuyển hướng về /")
    void getLogin_WhenAlreadyLoggedInAsCandidate_ShouldRedirectToCandidateHome() {
        fakeCurrentUserService.current = new CurrentUser(6L, "candidate1", "Candidate 1", RoleCode.CANDIDATE);
        Model model = new ConcurrentModel();

        String view = authController.loginPage(model);

        assertThat(view).isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("POST /login: Form có lỗi validation -> Trả về view auth/login")
    void postLogin_ValidationErrors_ShouldReturnLoginPage() {
        LoginRequest form = new LoginRequest("", "");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("identifier", "NotBlank", "Vui lòng nhập tên đăng nhập hoặc email");

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = authController.login(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("auth/login");
    }

    @Test
    @DisplayName("POST /login: Đăng nhập thành công -> Flash success và chuyển hướng theo role")
    void postLogin_Success_ShouldRedirectToRoleHome() {
        LoginRequest form = new LoginRequest("hr1", "Test@123");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        fakeUserService.responseToReturn = new UserResponse(
                2L, "hr1", "hr1@rms.local", "Trần Thu Hà", "0901000001", RoleCode.HR, UserStatus.ACTIVE
        );

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = authController.login(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("redirect:/hr/jobs");
        assertThat(ra.getFlashAttributes()).containsKey("success");
    }

    @Test
    @DisplayName("POST /login: Sai tài khoản/mật khẩu -> Trả về auth/login với errorMessage")
    void postLogin_BusinessException_ShouldReturnLoginPageWithErrorMessage() {
        LoginRequest form = new LoginRequest("wrong", "wrong");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        fakeUserService.exceptionToThrow = new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng.");

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = authController.login(form, bindingResult, model, ra);

        assertThat(view).isEqualTo("auth/login");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Tên đăng nhập hoặc mật khẩu không đúng.");
    }
}
