package com.group3.rms.common.config;

import com.group3.rms.common.security.RmsUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

/**
 * Cấu hình chung của ứng dụng — mọi bean / @Enable... dùng chung đặt tại đây, không tạo thêm file config lẻ.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AppConfig {

    /**
     * Tên người thực hiện, tự điền vào created_by / updated_by của BaseEntity.
     * Sprint 3: lấy username từ Spring Security nếu đã xác thực, ngược lại dùng "system".
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof RmsUserDetails details) {
                return Optional.of(details.username());
            }
            return Optional.of("system");
        };
    }

    /** Băm / so mật khẩu (BCrypt). Inject vào service đăng ký, tạo tài khoản, đăng nhập, đổi mật khẩu. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
