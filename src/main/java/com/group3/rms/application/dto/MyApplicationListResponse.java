package com.group3.rms.application.dto;

import com.group3.rms.application.entity.ApplicationStatus;

import java.util.List;
import java.util.Map;









public record MyApplicationListResponse(
        ApplicationStatus selectedStatus,
        List<MyApplicationResponse> applications,
        long totalCount,
        Map<ApplicationStatus, Long> statusCounts
) {
}
