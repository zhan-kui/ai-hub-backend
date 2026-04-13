package com.aihub.repository;

import com.aihub.entity.DeptRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeptRoleRepository extends JpaRepository<DeptRole, Long> {

    List<DeptRole> findAllByDeptId(Long deptId);

    void deleteByDeptId(Long deptId);

    void deleteByRoleId(Long roleId);
}
