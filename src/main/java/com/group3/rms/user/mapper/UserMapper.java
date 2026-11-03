package com.group3.rms.user.mapper;

import com.group3.rms.user.dto.UserResponse;
import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "roles", qualifiedByName = "mapPrimaryRole")
    UserResponse toResponse(User user);

    @Named("mapPrimaryRole")
    default RoleCode mapPrimaryRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.iterator().next().getCode();
    }
}
