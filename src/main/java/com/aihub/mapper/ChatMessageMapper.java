package com.aihub.mapper;

import com.aihub.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    List<ChatMessage> selectByConversationId(@Param("conversationId") Long conversationId);
}