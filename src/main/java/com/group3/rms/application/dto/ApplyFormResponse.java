package com.group3.rms.application.dto;

/** Phần chỉ đọc của SCR-15: tên vị trí, họ tên và email ứng viên. */
public record ApplyFormResponse(
        Long jobId,
        String jobTitle,
        String candidateFullName,
        String candidateEmail
) {
}
