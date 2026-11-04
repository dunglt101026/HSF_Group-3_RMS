package com.group3.rms.interview.dto;

import java.util.List;

/** Phần chỉ đọc và danh sách lựa chọn của SCR-18. */
public record InterviewScheduleFormResponse(
        Long applicationId,
        String candidateName,
        String jobTitle,
        int nextRoundNo,
        List<InterviewerOptionResponse> interviewers
) {
}
