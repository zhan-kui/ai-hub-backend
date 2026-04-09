package com.aihub.repository;

import com.aihub.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByIdAndDeletedFalse(Long id);

    boolean existsByUsernameAndDeletedFalse(String username);

    long countByDeletedFalse();

    @Modifying
    @Transactional
    @Query("update User u set u.lastLoginAt = :lastLoginAt where u.id = :userId")
    int updateLastLoginTime(@Param("userId") Long userId,
                            @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
