package com.group3.rms.interview.dto;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.interview.entity.InterviewStatus;

import java.time.LocalDateTime;

/** Một dòng trong bảng "Lịch phỏng vấn của tôi" (SCR-19). */
public record MyInterviewResponse(
        Long id,
        Long applicationId,
        LocalDateTime scheduledAt,
        String candidateName,
        String jobTitle,
        Integer roundNo,
        String location,
        InterviewStatus status,
        ApplicationStatus applicationStatus,
        Integer rating,
        boolean canEvaluate
) {
}
