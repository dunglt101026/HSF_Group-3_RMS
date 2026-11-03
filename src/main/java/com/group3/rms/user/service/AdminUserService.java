package com.group3.rms.user.service;

import com.group3.rms.user.dto.AdminUserSummaryResponse;
import org.springframework.data.domain.Page;

public interface AdminUserService {

    Page<AdminUserSummaryResponse> searchUsers(String keyword, String role, String status, String page);
}
