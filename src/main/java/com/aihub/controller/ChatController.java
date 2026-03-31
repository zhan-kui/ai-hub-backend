package com.aihub.controller;

import com.aihub.annotation.RequireResource;
import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.common.result.R;
import com.aihub.dify.model.ChatRequest;
import com.aihub.dify.service.DifyChatService;
import com.aihub.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "聊天管理")
public class ChatController {

    private final DifyChatService difyChatService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public SseEmitter streamChat(@RequestParam Long appId,
                                 @Valid @RequestBody ChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        difyChatService.streamChat(appId, userId, request, emitter);
        return emitter;
    }

    @PostMapping("/stop")
    @Operation(summary = "停止生成")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Void> stopGeneration(@RequestParam Long appId,
                                  @RequestParam String taskId) {
        Long userId = SecurityUtils.getCurrentUserId();
        difyChatService.stopGeneration(appId, userId, taskId);
        return R.ok();
    }

    @PostMapping("/feedback")
    @Operation(summary = "消息反馈（点赞/踩）")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Void> feedback(@RequestParam Long appId,
                            @RequestParam String messageId,
                            @RequestParam String rating) {
        Long userId = SecurityUtils.getCurrentUserId();
        difyChatService.feedback(appId, userId, messageId, rating);
        return R.ok();
    }

    @GetMapping("/suggested")
    @Operation(summary = "获取建议问题")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Object> suggested(@RequestParam Long appId,
                               @RequestParam String messageId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(difyChatService.getSuggested(appId, userId, messageId));
    }

    @PostMapping("/audio-to-text")
    @Operation(summary = "语音转文字")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Object> audioToText(@RequestParam Long appId,
                                 @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(difyChatService.audioToText(appId, userId, file));
    }
}