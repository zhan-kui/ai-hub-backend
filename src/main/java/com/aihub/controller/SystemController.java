package com.aihub.controller;

import com.aihub.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/system")
@Tag(name = "系统管理")
public class SystemController {

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public R<Map<String, Object>> health() {
        return R.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now()
        ));
    }
}