package com.aihub.service;

import com.aihub.entity.ChatMessage;
import com.aihub.entity.Conversation;
import com.aihub.mapper.ChatMessageMapper;
import com.aihub.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageMapper chatMessageMapper;

    @Transactional(rollbackFor = Exception.class)
    @Async
    public void saveRound(Long appId,
                          Long userId,
                          String query,
                          String thought,
                          String answer,
                          String difyConversationId,
                          String difyMessageId,
                          Map<String, Object> metadata) {
        if (!StringUtils.hasText(query) && !StringUtils.hasText(answer)) {
            return;
        }

        Conversation conversation = resolveConversation(userId, appId, difyConversationId, query);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setUserId(userId);
        message.setAppConfigId(appId);
        message.setDifyConversationId(difyConversationId);
        message.setDifyMessageId(difyMessageId);
        message.setRole("assistant");
        message.setQuery(query);
        message.setThought(thought);
        message.setAnswer(answer);
        message.setFeedback(null);
        message.setDeleted(false);
        message.setPromptTokens(extractInt(metadata, "prompt_tokens"));
        message.setCompletionTokens(extractInt(metadata, "completion_tokens"));
        message.setTotalTokens(extractInt(metadata, "total_tokens"));
        message.setTotalPrice(extractDecimal(metadata, "total_price"));
        message.setLatency(extractDecimal(metadata, "latency"));

        chatMessageMapper.insert(message);
    }

    private Conversation resolveConversation(Long userId,
                                             Long appId,
                                             String difyConversationId,
                                             String query) {
        Conversation conversation = null;
        if (StringUtils.hasText(difyConversationId)) {
            conversation = conversationRepository
                    .findByDifyConversationIdAndDeletedFalse(difyConversationId)
                    .filter(conv -> userId.equals(conv.getUserId()))
                    .orElse(null);
        }

        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setAppConfigId(appId);
            conversation.setDifyConversationId(difyConversationId);
            conversation.setTitle(buildTitle(query));
            conversation.setStatus(1);
            conversation.setDeleted(false);
        } else {
            if (!StringUtils.hasText(conversation.getTitle())) {
                conversation.setTitle(buildTitle(query));
            }
            conversation.setUpdatedAt(LocalDateTime.now());
        }

        return conversationRepository.save(conversation);
    }

    private String buildTitle(String query) {
        if (!StringUtils.hasText(query)) {
            return "新对话";
        }
        return query.length() > 30 ? query.substring(0, 30) : query;
    }

    private Integer extractInt(Map<String, Object> metadata, String key) {
        Object value = findValue(metadata, key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal extractDecimal(Map<String, Object> metadata, String key) {
        Object value = findValue(metadata, key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @SuppressWarnings("unchecked")
    private Object findValue(Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        if (metadata.containsKey(key)) {
            return metadata.get(key);
        }
        Object usage = metadata.get("usage");
        if (usage instanceof Map<?, ?> usageMap) {
            return ((Map<String, Object>) usageMap).get(key);
        }
        return null;
    }
}