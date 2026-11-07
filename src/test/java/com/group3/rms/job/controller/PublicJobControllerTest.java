package com.group3.rms.job.controller;

import com.group3.rms.job.service.PublicJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicJobControllerTest {

    @Mock
    PublicJobService publicJobService;

    PublicJobController controller;

    @BeforeEach
    void setUp() {
        controller = new PublicJobController(publicJobService);
    }

    @Test
    @DisplayName("Tham số hợp lệ được chuyển đúng cho service")
    void validParams() {
        Model model = new ConcurrentModel();

        String view = controller.listJobs("java", "3", "Hà Nội", "2", model);

        assertThat(view).isEqualTo("career/list");
        verify(publicJobService).searchJobs("java", 3L, "Hà Nội", 2);
        assertThat(model.getAttribute("departmentId")).isEqualTo(3L);
    }

    @Test
    @DisplayName("Phòng ban, số trang sửa tay thành chữ → bỏ qua, về trang đầu")
    void invalidParams() {
        controller.listJobs(null, "abc", null, "xyz", new ConcurrentModel());

        verify(publicJobService).searchJobs(null, null, null, 0);
    }

    @Test
    @DisplayName("Số trang âm hoặc quá lớn → không lỗi")
    void outOfRangePage() {
        controller.listJobs(null, null, null, "-5", new ConcurrentModel());
        verify(publicJobService).searchJobs(null, null, null, 0);

        controller.listJobs(null, null, null, "99999999999", new ConcurrentModel());
        verify(publicJobService).searchJobs(null, null, null, Integer.MAX_VALUE);
    }
}
