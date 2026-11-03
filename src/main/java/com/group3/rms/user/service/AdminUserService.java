package com.group3.rms.user.service;

import com.group3.rms.user.dto.AdminUserCreateRequest;
import com.group3.rms.user.dto.AdminUserSummaryResponse;
import org.springframework.data.domain.Page;

public interface AdminUserService {

    Page<AdminUserSummaryResponse> searchUsers(String keyword, String role, String status, String page);

    void checkCreatePermission();

    void checkAccountAvailability(String username, String email);

    Long createUser(AdminUserCreateRequest request);
}
