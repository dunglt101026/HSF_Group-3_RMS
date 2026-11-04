package com.group3.rms.interview.dto;

import com.group3.rms.interview.entity.InterviewStatus;

import java.time.LocalDateTime;

/**
 * Một vòng phỏng vấn trong bảng ở SCR-17.
 * {@code rating}, {@code feedback} để trống khi người xem không được thấy đánh giá của vòng này.
 */
public record InterviewRoundResponse(
        Long id,
        Integer roundNo,
        String interviewerName,
        LocalDateTime scheduledAt,
        String location,
        InterviewStatus status,
        Integer rating,
        String feedback,
        LocalDateTime evaluatedAt,
        boolean evaluationVisible,
        boolean canEvaluate
) {
}
