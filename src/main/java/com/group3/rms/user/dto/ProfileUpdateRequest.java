package com.group3.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName,

        @Pattern(regexp = "^$|[0-9]{9,15}", message = "Số điện thoại phải gồm 9–15 chữ số")
        String phone
) {}
