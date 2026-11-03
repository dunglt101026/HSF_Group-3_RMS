package com.group3.rms.user.mapper;

import com.group3.rms.user.dto.AdminUserCreateRequest;
import com.group3.rms.user.dto.AdminUserSummaryResponse;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    @Mapping(target = "role", source = "role")
    AdminUserSummaryResponse toSummaryResponse(User user, RoleCode role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toUser(AdminUserCreateRequest request);
}
