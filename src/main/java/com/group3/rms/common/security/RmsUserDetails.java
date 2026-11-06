package com.group3.rms.common.security;

import com.group3.rms.user.entity.RoleCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Wrapper để Spring Security nhận diện user qua SecurityContext.
 * Lưu thêm CurrentUser để controller truy xuất thông tin nghiệp vụ.
 */
public record RmsUserDetails(
        Long id,
        String username,
        String fullName,
        String passwordHash,
        RoleCode role,
        boolean enabled
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** Chuyển sang CurrentUser dùng trong logic nghiệp vụ. */
    public CurrentUser toCurrentUser() {
        return new CurrentUser(id, username, fullName, role);
    }
}
