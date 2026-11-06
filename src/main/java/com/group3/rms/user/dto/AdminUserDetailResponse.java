package com.group3.rms.user.dto;

import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long id,
        String fullName,
        String username,
        String email,
        String phone,
        RoleCode role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long jobCount
) {}
