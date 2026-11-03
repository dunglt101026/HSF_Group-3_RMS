package com.group3.rms.user.mapper;

import com.group3.rms.user.dto.AdminUserSummaryResponse;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    @Mapping(target = "role", source = "role")
    AdminUserSummaryResponse toSummaryResponse(User user, RoleCode role);
}
