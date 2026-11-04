package com.group3.rms.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/** Form SCR-18: lên lịch một vòng phỏng vấn (UC-23). */
public record InterviewCreateRequest(

        @NotNull(message = "Vui lòng chọn người phỏng vấn.")
        Long interviewerId,

        @NotNull(message = "Vui lòng chọn ngày phỏng vấn.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate scheduledDate,

        @NotNull(message = "Vui lòng chọn giờ phỏng vấn.")
        @DateTimeFormat(pattern = "HH:mm")
        LocalTime scheduledTime,

        @NotBlank(message = "Vui lòng nhập địa điểm hoặc link họp.")
        @Size(max = 500, message = "Địa điểm tối đa 500 ký tự.")
        String location
) {

    public static InterviewCreateRequest empty() {
        return new InterviewCreateRequest(null, null, null, null);
    }
}
