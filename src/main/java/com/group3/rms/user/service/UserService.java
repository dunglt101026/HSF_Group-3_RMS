package com.group3.rms.user.service;

import com.group3.rms.user.dto.LoginRequest;
import com.group3.rms.user.dto.PasswordChangeRequest;
import com.group3.rms.user.dto.ProfileUpdateRequest;
import com.group3.rms.user.dto.RegisterRequest;
import com.group3.rms.user.dto.UserResponse;

public interface UserService {

    UserResponse login(LoginRequest request);

    UserResponse register(RegisterRequest request);

    void changePassword(PasswordChangeRequest request);

    UserResponse getCurrentUserProfile();

    UserResponse updateProfile(ProfileUpdateRequest request);
}
