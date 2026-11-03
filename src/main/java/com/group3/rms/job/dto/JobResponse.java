
package com.group3.rms.job.dto;

import com.group3.rms.job.entity.JobStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JobResponse(
        Long id,
        String title,
        String description,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDate deadline,
        JobStatus status,
        LocalDateTime publishedAt,
        String departmentName,
        String ownerName,
        List<String> skills,
        Long departmentId,
        List<Long> skillIds
) {
}
