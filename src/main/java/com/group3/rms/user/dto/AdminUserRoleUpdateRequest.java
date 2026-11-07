package com.group3.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminUserRoleUpdateRequest(
        @NotBlank(message = "Chỉ được đổi vai trò giữa HR và INTERVIEWER.")
        @Pattern(regexp = "^$|HR|INTERVIEWER", message = "Chỉ được đổi vai trò giữa HR và INTERVIEWER.")
        String role
) {}
