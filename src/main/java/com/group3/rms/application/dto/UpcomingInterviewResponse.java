package com.group3.rms.application.dto;

import java.time.LocalDateTime;

/** Một buổi phỏng vấn sắp tới ở SCR-16. Không có tên người phỏng vấn và đánh giá (UC-19). */
public record UpcomingInterviewResponse(
        Long applicationId,
        Integer roundNo,
        LocalDateTime scheduledAt,
        String location
) {
}
