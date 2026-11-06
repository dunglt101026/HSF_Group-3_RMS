package com.group3.rms.common.config;

import com.group3.rms.common.security.RmsUserDetails;
import com.group3.rms.common.security.RmsUserDetailsService;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.common.security.UserSessionSyncFilter;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * Cấu hình Spring Security cho Sprint 3.
 * <p>
 * Chiến lược: giữ nguyên flow login thủ công qua AuthController,
 * Spring Security chịu trách nhiệm bảo vệ URL theo role và quản lý session.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RmsUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── Session Sync (UC-08, UC-09) ────────────────────────────────
            .addFilterAfter(new UserSessionSyncFilter(userRepository, currentUserService), SecurityContextHolderFilter.class)

            // ── Authorization ──────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Static resources & public pages
                .requestMatchers(
                    "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/favicon.ico", "/error", "/error/**"
                ).permitAll()
                // Auth pages
                .requestMatchers("/login", "/register").permitAll()
                // Việc làm công khai (UC-16, UC-17)
                .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/*").permitAll()
                // Chỉ Candidate: nộp hồ sơ, hồ sơ của tôi (UC-18, UC-19, UC-20)
                .requestMatchers("/jobs/*/apply", "/my/**").hasRole("CANDIDATE")
                // Admin only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // HR only
                .requestMatchers("/hr/**").hasRole("HR")
                // Xử lý hồ sơ ứng tuyển (UC-21, UC-22)
                .requestMatchers("/applications/**").hasAnyRole("HR", "INTERVIEWER")
                // Interviewer và HR (UC-24: lịch phỏng vấn, service chặn Admin)
                .requestMatchers("/interviews/**").hasAnyRole("INTERVIEWER", "HR")
                // Any authenticated user
                .requestMatchers("/profile/**").authenticated()
                // Trang chủ ứng viên
                .requestMatchers("/").permitAll()
                // Everything else requires login
                .anyRequest().authenticated()
            )

            // ── Form Login ─────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("identifier")
                .passwordParameter("password")
                .successHandler(authSuccessHandler())
                .failureHandler(authFailureHandler())
                .permitAll()
            )

            // ── Logout ─────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ── Exception Handling ─────────────────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect(request.getContextPath() + "/login");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    request.getRequestDispatcher("/error/403").forward(request, response);
                })
            )

            // ── UserDetailsService & PasswordEncoder ───────────────────────
            .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * Sau khi Spring Security xác thực thành công:
     * 1. Lưu CurrentUser vào session (tương thích code nghiệp vụ hiện có)
     * 2. Redirect về "/", HomeController chuyển tiếp theo vai trò
     */
    private AuthenticationSuccessHandler authSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            RmsUserDetails details = (RmsUserDetails) authentication.getPrincipal();
            CurrentUser currentUser = details.toCurrentUser();
            currentUserService.set(currentUser);

            log.info("Người dùng '{}' đăng nhập thành công với vai trò {}", currentUser.username(), currentUser.role());

            // UC-01: Candidate quay lại trang đang xem dở nếu có; nhân viên luôn về trang theo vai trò.
            RequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (currentUser.role() == RoleCode.CANDIDATE && savedRequest != null) {
                response.sendRedirect(savedRequest.getRedirectUrl());
                return;
            }
            requestCache.removeRequest(request, response);

            response.sendRedirect(request.getContextPath() + "/");
        };
    }

    /**
     * Khi xác thực thất bại: redirect về /login với thông báo lỗi phù hợp.
     */
    private AuthenticationFailureHandler authFailureHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) -> {
            String message;
            if (exception instanceof DisabledException) {
                message = "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.";
            } else {
                message = "Tên đăng nhập hoặc mật khẩu không đúng.";
            }
            log.warn("Đăng nhập thất bại: {}", exception.getMessage());
            request.getSession().setAttribute("loginError", message);
            response.sendRedirect(request.getContextPath() + "/login?error");
        };
    }

    /**
     * Sau khi logout: xóa CurrentUser khỏi session, redirect về /login.
     */
    private LogoutSuccessHandler logoutSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            currentUserService.clear();
            log.info("Người dùng đã đăng xuất");
            response.sendRedirect(request.getContextPath() + "/login?logout");
        };
    }
}
