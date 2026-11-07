package com.group3.rms.common.storage;

import com.group3.rms.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    LocalFileStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalFileStorageService(tempDir.toString());
    }

    private MockMultipartFile file(String name, int size) {
        return new MockMultipartFile("cvFile", name, "application/octet-stream", new byte[size]);
    }

    @Test
    @DisplayName("Lưu file hợp lệ với tên ngẫu nhiên, đọc lại được, xóa được")
    void storeLoadDelete() throws IOException {
        String path = service.store(file("CV của An.PDF", 10));

        assertThat(path).endsWith(".pdf").doesNotContain("An");
        Resource resource = service.load(path);
        assertThat(resource.contentLength()).isEqualTo(10);

        service.delete(path);
        assertThat(Files.exists(tempDir.resolve(path))).isFalse();
    }

    @Test
    @DisplayName("Không chọn file → lỗi dưới ô cvFile")
    void emptyFile() {
        assertThatThrownBy(() -> service.store(file("cv.pdf", 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vui lòng chọn file CV.")
                .extracting("field").isEqualTo("cvFile");
        assertThatThrownBy(() -> service.store(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Sai định dạng → lỗi dưới ô cvFile")
    void wrongExtension() {
        assertThatThrownBy(() -> service.store(file("cv.exe", 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CV chỉ nhận định dạng PDF, DOC hoặc DOCX.")
                .extracting("field").isEqualTo("cvFile");
    }

    @Test
    @DisplayName("Đúng 5 MB được nhận, quá 5 MB bị từ chối")
    void sizeLimit() {
        int fiveMb = 5 * 1024 * 1024;

        assertThat(service.store(file("cv.docx", fiveMb))).endsWith(".docx");
        assertThatThrownBy(() -> service.store(file("cv.docx", fiveMb + 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CV không được vượt quá 5 MB.");
    }

    @Test
    @DisplayName("File không còn trên máy chủ → báo lỗi")
    void loadMissingFile() {
        assertThatThrownBy(() -> service.load("khong-co.pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("File CV không còn trên máy chủ.");
    }

    @Test
    @DisplayName("Đường dẫn trỏ ra ngoài thư mục lưu CV → không đọc được")
    void pathTraversal() throws IOException {
        Path outside = Files.writeString(tempDir.getParent().resolve("ngoai-" + tempDir.getFileName() + ".pdf"), "x");
        try {
            assertThatThrownBy(() -> service.load("../" + outside.getFileName()))
                    .isInstanceOf(BusinessException.class);
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    @DisplayName("CV mẫu của seed.sql ('seed/...') đọc được từ thư mục CV mẫu, không cần chép vào uploads")
    void loadSampleCvOfSeedData() {
        assertThat(service.load("seed/sample_cv_java_backend.pdf").exists()).isTrue();
    }

    @Test
    @DisplayName("Đường dẫn 'seed/' không có file hoặc đi lên thư mục khác → không đọc được")
    void sampleCvRejectsMissingOrTraversal() {
        assertThatThrownBy(() -> service.load("seed/khong-co.pdf")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.load("seed/../application.properties")).isInstanceOf(BusinessException.class);
    }
}
