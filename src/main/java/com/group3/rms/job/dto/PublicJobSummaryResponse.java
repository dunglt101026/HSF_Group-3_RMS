package com.group3.rms.job.dto;

import java.time.LocalDate;


public record PublicJobSummaryResponse(
        Long id,
        String title,
        String departmentName,
        String location,
        String salaryText,
        LocalDate deadline
) {
}
