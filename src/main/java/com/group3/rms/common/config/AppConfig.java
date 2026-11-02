package com.group3.rms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

/**
 * Cấu hình chung của ứng dụng — mọi bean / @Enable... dùng chung đặt tại đây, không tạo thêm file config lẻ.
 * Về sau: thêm @EnableScheduling (tự đóng tin hết hạn) vào class này.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AppConfig {

    /**
     * Tên người thực hiện, tự điền vào created_by / updated_by của BaseEntity.
     * TODO: khi có CurrentUserService, trả về username người đang đăng nhập; chưa đăng nhập thì "system".
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("system");
    }

    /** Băm / so mật khẩu (BCrypt). Inject vào service đăng ký, tạo tài khoản, đăng nhập, đổi mật khẩu. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
