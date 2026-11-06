
package com.group3.rms.job.dto;

import com.group3.rms.job.entity.JobStatus;

import java.time.LocalDate;

public record JobSummaryResponse(
        Long id,
        String title,
        String departmentName,
        Long ownerId,
        String ownerName,
        JobStatus status,
        LocalDate deadline,
        long applicationCount
) {
}
