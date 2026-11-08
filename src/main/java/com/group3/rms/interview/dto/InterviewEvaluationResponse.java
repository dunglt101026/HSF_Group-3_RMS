package com.group3.rms.interview.dto;

import com.group3.rms.interview.entity.InterviewStatus;

import java.time.LocalDateTime;


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


    public boolean evaluated() {
        return status == InterviewStatus.EVALUATED;
    }
}
