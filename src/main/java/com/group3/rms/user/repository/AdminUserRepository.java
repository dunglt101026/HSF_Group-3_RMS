package com.group3.rms.user.repository;

import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<User, Long> {

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(Long id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query(value = """
            select u.id from User u
            where (:keyword is null
                or lower(u.username) like :keyword escape '!'
                or lower(u.email) like :keyword escape '!'
                or lower(u.fullName) like :keyword escape '!')
            and (:role is null or exists (
                select r.id from User roleUser join roleUser.roles r
                where roleUser.id = u.id and r.code = :role))
            and (:status is null or u.status = :status)
            """, countQuery = """
            select count(u.id) from User u
            where (:keyword is null
                or lower(u.username) like :keyword escape '!'
                or lower(u.email) like :keyword escape '!'
                or lower(u.fullName) like :keyword escape '!')
            and (:role is null or exists (
                select r.id from User roleUser join roleUser.roles r
                where roleUser.id = u.id and r.code = :role))
            and (:status is null or u.status = :status)
            """)
    Page<Long> searchUserIds(@Param("keyword") String keyword,
                             @Param("role") RoleCode role,
                             @Param("status") UserStatus status,
                             Pageable pageable);

    @Query("""
            select distinct u from User u
            left join fetch u.roles
            where u.id in :ids
            order by u.createdAt desc, u.id desc
            """)
    List<User> findAllWithRolesByIdIn(@Param("ids") List<Long> ids);
}
