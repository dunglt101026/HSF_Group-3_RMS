package com.group3.rms.interview.dto;

/** Kết quả lên lịch, dùng cho thông báo "Đã lên lịch vòng n với …". */
public record InterviewScheduledResponse(
        Long id,
        int roundNo,
        String interviewerName
) {
}
