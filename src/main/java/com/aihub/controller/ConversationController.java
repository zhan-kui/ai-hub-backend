package com.aihub.controller;

import com.aihub.annotation.RequireResource;
import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.common.result.R;
import com.aihub.dify.service.DifyConversationService;
import com.aihub.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@Tag(name = "对话管理")
public class ConversationController {

    private final DifyConversationService difyConversationService;

    @GetMapping
    @Operation(summary = "获取对话列表")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Object> list(@RequestParam Long appId,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "20") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(difyConversationService.listConversations(appId, userId, page, limit));
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "获取对话消息列表")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Object> messages(@PathVariable String conversationId,
                              @RequestParam Long appId,
                              @RequestParam(required = false) String firstId,
                              @RequestParam(defaultValue = "20") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(difyConversationService.listMessages(appId, userId, conversationId, firstId, limit));
    }

    @PutMapping("/{conversationId}/name")
    @Operation(summary = "重命名对话")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Object> rename(@PathVariable String conversationId,
                            @RequestParam Long appId,
                            @RequestParam(required = false) String name,
                            @RequestParam(defaultValue = "false") Boolean autoGenerate) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(difyConversationService.rename(appId, userId, conversationId, name, autoGenerate));
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "删除对话")
    @RequireResource(type = ResourceTypeEnum.APP, paramName = "appId")
    public R<Void> delete(@PathVariable String conversationId,
                          @RequestParam Long appId) {
        Long userId = SecurityUtils.getCurrentUserId();
        difyConversationService.delete(appId, userId, conversationId);
        return R.ok();
    }
}