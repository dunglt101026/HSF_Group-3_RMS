package com.group3.rms.interview.dto;

import com.group3.rms.interview.entity.InterviewStatus;

import java.time.LocalDateTime;





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
