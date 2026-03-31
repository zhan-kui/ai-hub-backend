package com.aihub.repository;

import com.aihub.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCodeAndDeletedFalse(String code);

    List<Role> findAllByDeletedFalseOrderBySortAsc();
}