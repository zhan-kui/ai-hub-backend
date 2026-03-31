package com.aihub.repository;

import com.aihub.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findAllByDeletedFalseAndEnabledTrueOrderBySortAsc();

    List<KnowledgeBase> findAllByIdInAndDeletedFalseAndEnabledTrue(List<Long> ids);
}