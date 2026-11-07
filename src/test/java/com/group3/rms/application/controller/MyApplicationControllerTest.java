package com.group3.rms.application.controller;

import com.group3.rms.application.dto.CvFileResponse;
import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.application.service.CandidateApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyApplicationControllerTest {

    @Mock
    CandidateApplicationService candidateApplicationService;

    MyApplicationController controller;

    @BeforeEach
    void setUp() {
        controller = new MyApplicationController(candidateApplicationService);
    }

    @Test
    @DisplayName("Tab trạng thái hợp lệ được lọc đúng")
    void validStatus() {
        controller.myApplications("HIRED", new ConcurrentModel());

        verify(candidateApplicationService).getMyApplications(ApplicationStatus.HIRED);
    }

    @Test
    @DisplayName("Tab trạng thái sửa tay thành giá trị sai → hiện tab Tất cả")
    void invalidStatus() {
        controller.myApplications("FOO", new ConcurrentModel());

        verify(candidateApplicationService).getMyApplications(null);
    }

    @Test
    @DisplayName("Xem CV PDF: mở ngay trên trình duyệt, giữ tên file tiếng Việt")
    void viewPdf() {
        Resource file = new ByteArrayResource(new byte[]{1});
        when(candidateApplicationService.getMyCv(1L)).thenReturn(new CvFileResponse(file, "CV Phạm Dung.pdf"));

        ResponseEntity<Resource> response = controller.viewCv(1L);

        assertThat(response.getBody()).isSameAs(file);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("inline")
                .contains("UTF-8''CV%20Ph%E1%BA%A1m%20Dung.pdf");
    }
}
