package com.group3.rms.user.repository;

import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(RoleCode code);
}
