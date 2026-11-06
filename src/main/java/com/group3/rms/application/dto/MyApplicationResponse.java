package com.group3.rms.application.dto;

import com.group3.rms.application.entity.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Một dòng trong bảng "Hồ sơ ứng tuyển của tôi" (SCR-16). */
public record MyApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        LocalDateTime appliedAt,
        ApplicationStatus status,
        List<UpcomingInterviewResponse> upcomingInterviews
) {

    /** UC-20: chỉ rút được khi APPLIED hoặc INTERVIEW. Service vẫn kiểm tra lại khi rút. */
    public boolean canWithdraw() {
        return status == ApplicationStatus.APPLIED || status == ApplicationStatus.INTERVIEW;
    }
}
