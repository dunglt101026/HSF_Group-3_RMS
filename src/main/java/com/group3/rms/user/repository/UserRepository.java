package com.group3.rms.user.repository;

import com.group3.rms.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Load User kèm roles trong một lần JOIN — dùng cho UserSessionSyncFilter
     * để tránh LazyInitializationException khi truy cập roles ngoài transaction.
     */
    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);
}
