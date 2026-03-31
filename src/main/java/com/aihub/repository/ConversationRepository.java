package com.aihub.repository;

import com.aihub.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findAllByUserIdAndDeletedFalseOrderByUpdatedAtDesc(Long userId);

    Optional<Conversation> findByDifyConversationIdAndDeletedFalse(String difyConversationId);
}