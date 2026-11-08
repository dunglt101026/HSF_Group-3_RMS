package com.group3.rms.common.security;

import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;







@Slf4j
@RequiredArgsConstructor
public class UserSessionSyncFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof RmsUserDetails userDetails) {
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String path = uri.substring(contextPath.length());



            Optional<User> optionalUser = userRepository.findWithRolesById(userDetails.id());


            if (optionalUser.isEmpty() || optionalUser.get().getStatus() != UserStatus.ACTIVE) {
                log.warn("Tài khoản '{}' (ID={}) đã bị khóa hoặc không còn tồn tại. Hủy phiên làm việc.",
                        userDetails.username(), userDetails.id());

                SecurityContextHolder.clearContext();
                currentUserService.clear();

                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }

                if ("/login".equals(path) || "/logout".equals(path)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                response.sendRedirect(request.getContextPath() + "/login?locked");
                return;
            }


            User user = optionalUser.get();
            RoleCode dbRole = user.getRoles().stream()
                    .findFirst()
                    .map(Role::getCode)
                    .orElse(RoleCode.CANDIDATE);

            if (dbRole != userDetails.role() || !user.getFullName().equals(userDetails.fullName())) {
                log.info("Cập nhật lại quyền/thông tin trong phiên cho '{}': role cũ={}, role mới={}",
                        user.getUsername(), userDetails.role(), dbRole);

                RmsUserDetails updatedDetails = new RmsUserDetails(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getPasswordHash(),
                        dbRole,
                        true
                );

                UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                        updatedDetails,
                        auth.getCredentials(),
                        updatedDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
                currentUserService.set(updatedDetails.toCurrentUser());
            }
        }

        filterChain.doFilter(request, response);
    }
}
