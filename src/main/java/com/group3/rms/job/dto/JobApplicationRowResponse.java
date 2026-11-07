package com.group3.rms.job.dto;

import com.group3.rms.application.entity.ApplicationStatus;

import java.time.LocalDateTime;

/** Một dòng trong bảng hồ sơ ứng viên của tin (SCR-12). */
public record JobApplicationRowResponse(
        Long id,
        String candidateName,
        LocalDateTime appliedAt,
        ApplicationStatus status,
        long interviewCount
) {}
