package com.group3.rms.job.dto;

import java.time.LocalDate;
import java.util.List;


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
