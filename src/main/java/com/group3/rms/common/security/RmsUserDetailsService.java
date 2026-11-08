package com.group3.rms.common.security;

import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;





@Slf4j
@Service
@RequiredArgsConstructor
public class RmsUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> {
                    log.warn("Spring Security: Không tìm thấy tài khoản '{}'", identifier);
                    return new UsernameNotFoundException("Tên đăng nhập hoặc mật khẩu không đúng.");
                });

        RoleCode primaryRole = user.getRoles().stream()
                .findFirst()
                .map(Role::getCode)
                .orElse(RoleCode.CANDIDATE);

        boolean enabled = user.getStatus() == UserStatus.ACTIVE;

        return new RmsUserDetails(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPasswordHash(),
                primaryRole,
                enabled
        );
    }
}
