package com.group3.rms.application.dto;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * Form SCR-15: nộp hồ sơ (UC-18).
 * File CV được FileStorageService kiểm tra (định dạng, dung lượng) và báo lỗi dưới ô cvFile.
 */
public record ApplicationCreateRequest(

        MultipartFile cvFile,

        @Size(max = 2000, message = "Thư giới thiệu tối đa 2000 ký tự.")
        String coverLetter
) {

    public static ApplicationCreateRequest empty() {
        return new ApplicationCreateRequest(null, null);
    }
}
