package com.aihub.repository;

import com.aihub.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {

    List<AppConfig> findAllByDeletedFalseAndEnabledTrueOrderBySortAsc();

    List<AppConfig> findAllByIdInAndDeletedFalseAndEnabledTrue(List<Long> ids);
}