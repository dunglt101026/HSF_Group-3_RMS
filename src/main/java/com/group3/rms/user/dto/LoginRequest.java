package com.group3.rms.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc email")
        String identifier,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        String password
) {}
