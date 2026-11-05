package com.group3.rms.job.dto;

import java.time.LocalDate;

/** Một thẻ tin ở SCR-13. Không có thông tin nội bộ (HR phụ trách, số hồ sơ). */
public record PublicJobSummaryResponse(
        Long id,
        String title,
        String departmentName,
        String location,
        String salaryText,
        LocalDate deadline
) {
}
