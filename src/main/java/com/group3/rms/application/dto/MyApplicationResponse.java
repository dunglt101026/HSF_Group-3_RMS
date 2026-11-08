package com.group3.rms.application.dto;

import com.group3.rms.application.entity.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;


public record MyApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        LocalDateTime appliedAt,
        ApplicationStatus status,
        List<UpcomingInterviewResponse> upcomingInterviews
) {


    public boolean canWithdraw() {
        return status == ApplicationStatus.APPLIED || status == ApplicationStatus.INTERVIEW;
    }
}
