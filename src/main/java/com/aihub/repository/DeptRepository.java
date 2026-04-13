package com.aihub.repository;

import com.aihub.entity.Dept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeptRepository extends JpaRepository<Dept, Long> {

    List<Dept> findAllByDeletedFalseOrderBySortAscIdAsc();

    Optional<Dept> findByIdAndDeletedFalse(Long id);

    boolean existsByParentIdAndDeletedFalse(Long parentId);

    List<Dept> findAllByParentIdAndDeletedFalse(Long parentId);
}
