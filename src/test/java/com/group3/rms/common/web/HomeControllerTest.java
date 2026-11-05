package com.group3.rms.common.web;

import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.user.entity.RoleCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeControllerTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final HomeController controller = new HomeController(currentUserService);

    @ParameterizedTest
    @CsvSource({
            "ADMIN, redirect:/admin/users",
            "HR, redirect:/hr/jobs",
            "INTERVIEWER, redirect:/interviews/my",
            "CANDIDATE, redirect:/jobs"
    })
    @DisplayName("Đã đăng nhập → về trang theo vai trò")
    void redirectsByRole(RoleCode role, String expected) {
        when(currentUserService.get()).thenReturn(Optional.of(new CurrentUser(1L, "user", "User", role)));

        assertThat(controller.home(new ConcurrentModel(), new RedirectAttributesModelMap())).isEqualTo(expected);
    }

    @Test
    @DisplayName("Guest → trang việc làm")
    void guestGoesToJobs() {
        when(currentUserService.get()).thenReturn(Optional.empty());

        assertThat(controller.home(new ConcurrentModel(), new RedirectAttributesModelMap())).isEqualTo("redirect:/jobs");
    }

    @Test
    @DisplayName("Thông báo lỗi được giữ qua lần chuyển hướng theo vai trò")
    void keepsErrorMessage() {
        when(currentUserService.get()).thenReturn(Optional.empty());
        Model model = new ConcurrentModel();
        model.addAttribute("error", "Không thể chuyển hồ sơ");
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        controller.home(model, attributes);

        assertThat(attributes.getFlashAttributes().get("error")).isEqualTo("Không thể chuyển hồ sơ");
    }
}
