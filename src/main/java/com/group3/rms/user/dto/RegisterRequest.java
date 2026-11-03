package com.group3.rms.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Vui lòng nhập tên đăng nhập")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,50}$", message = "Tên đăng nhập gồm 4–50 ký tự chữ, số hoặc dấu gạch dưới")
        String username,

        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Địa chỉ email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$", message = "Mật khẩu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa và 1 chữ số")
        String password,

        @NotBlank(message = "Vui lòng xác nhận mật khẩu")
        String confirmPassword
) {
    @AssertTrue(message = "Mật khẩu xác nhận không khớp.")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) {
            return true;
        }
        return password.equals(confirmPassword);
    }
}
