package com.group3.rms.application.dto;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.interview.dto.InterviewRoundResponse;

import java.time.LocalDateTime;
import java.util.List;


public record ApplicationDetailResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String departmentName,
        String candidateFullName,
        String candidateEmail,
        String candidatePhone,
        String cvOriginalName,
        String coverLetter,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime statusChangedAt,
        List<InterviewRoundResponse> interviews,
        ApplicationActions actions
) {


    public boolean finished() {
        return status == ApplicationStatus.HIRED
                || status == ApplicationStatus.REJECTED
                || status == ApplicationStatus.WITHDRAWN;
    }
}
