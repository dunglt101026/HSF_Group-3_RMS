package com.group3.rms.job.dto;

import com.group3.rms.application.entity.ApplicationStatus;

import java.util.Map;

/**
 * Số hồ sơ của một tin theo từng trạng thái — dùng cho SCR-11, SCR-12.
 * "Tất cả" bao gồm cả hồ sơ đã rút (WITHDRAWN).
 */
public record JobApplicationStatsResponse(
        long total,
        long applied,
        long interviewing,
        long hired,
        long rejected,
        long withdrawn
) {

    public static JobApplicationStatsResponse from(Map<ApplicationStatus, Long> countsByStatus) {
        long applied = countsByStatus.getOrDefault(ApplicationStatus.APPLIED, 0L);
        long interviewing = countsByStatus.getOrDefault(ApplicationStatus.INTERVIEW, 0L);
        long hired = countsByStatus.getOrDefault(ApplicationStatus.HIRED, 0L);
        long rejected = countsByStatus.getOrDefault(ApplicationStatus.REJECTED, 0L);
        long withdrawn = countsByStatus.getOrDefault(ApplicationStatus.WITHDRAWN, 0L);

        return new JobApplicationStatsResponse(
                applied + interviewing + hired + rejected + withdrawn,
                applied,
                interviewing,
                hired,
                rejected,
                withdrawn
        );
    }
}
