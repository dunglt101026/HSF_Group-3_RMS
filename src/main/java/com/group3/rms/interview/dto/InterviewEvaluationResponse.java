package com.group3.rms.interview.dto;

import com.group3.rms.interview.entity.InterviewStatus;

import java.time.LocalDateTime;

/** Dữ liệu SCR-20: thông tin buổi phỏng vấn (chỉ đọc) và đánh giá nếu đã gửi. */
public record InterviewEvaluationResponse(
        Long id,
        Long applicationId,
        String candidateName,
        String jobTitle,
        Integer roundNo,
        LocalDateTime scheduledAt,
        String location,
        InterviewStatus status,
        Integer rating,
        String feedback,
        LocalDateTime evaluatedAt
) {

    /** Đã gửi đánh giá → màn hình ở chế độ chỉ đọc. */
    public boolean evaluated() {
        return status == InterviewStatus.EVALUATED;
    }
}
