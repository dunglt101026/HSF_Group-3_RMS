package com.group3.rms.interview.dto;

import java.util.List;


public record InterviewScheduleFormResponse(
        Long applicationId,
        String candidateName,
        String jobTitle,
        int nextRoundNo,
        List<InterviewerOptionResponse> interviewers
) {
}
