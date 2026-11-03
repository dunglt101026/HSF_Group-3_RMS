package com.group3.rms.user.dto;

import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.UserStatus;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        RoleCode role,
        UserStatus status
) {}
