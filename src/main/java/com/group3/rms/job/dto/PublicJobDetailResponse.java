package com.group3.rms.job.dto;

import java.time.LocalDate;
import java.util.List;

/** Dữ liệu SCR-14: chi tiết việc làm và vùng hành động. Không có thông tin nội bộ. */
public record PublicJobDetailResponse(
        Long id,
        String title,
        String departmentName,
        String location,
        String salaryText,
        LocalDate deadline,
        String description,
        List<String> skills,
        ApplyState applyState
) {
}
