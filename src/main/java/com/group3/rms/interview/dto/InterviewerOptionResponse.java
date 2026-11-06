package com.group3.rms.interview.dto;

import com.group3.rms.user.entity.RoleCode;

/** Một lựa chọn trong ô "Người phỏng vấn" ở SCR-18. */
public record InterviewerOptionResponse(
        Long id,
        String fullName,
        RoleCode role
) {
}
