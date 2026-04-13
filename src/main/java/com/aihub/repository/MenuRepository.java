package com.aihub.repository;

import com.aihub.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
/**
 * MenuRepository interface。
 * <p>该类型承担当前文件的核心职责，建议结合同包相关类一起阅读。
 */

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByIdAndDeletedFalse(Long id);

    List<Menu> findAllByDeletedFalseOrderBySortAscIdAsc();

    List<Menu> findAllByDeletedFalseAndPlatformInOrderBySortAscIdAsc(List<String> platforms);

    List<Menu> findAllByIdInAndDeletedFalseOrderBySortAscIdAsc(List<Long> ids);

    boolean existsByParentIdAndDeletedFalse(Long parentId);
}
