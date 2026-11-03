package com.group3.rms.common.security;

import com.group3.rms.user.entity.RoleCode;

public record CurrentUser(Long id, String username, String fullName, RoleCode role) {}
