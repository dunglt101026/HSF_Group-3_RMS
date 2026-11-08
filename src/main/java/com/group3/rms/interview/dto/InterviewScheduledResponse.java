package com.group3.rms.interview.dto;


public record InterviewScheduledResponse(
        Long id,
        int roundNo,
        String interviewerName
) {
}
