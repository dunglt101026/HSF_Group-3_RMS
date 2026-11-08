package com.group3.rms.interview.dto;

import com.group3.rms.user.entity.RoleCode;


public record InterviewerOptionResponse(
        Long id,
        String fullName,
        RoleCode role
) {
}
