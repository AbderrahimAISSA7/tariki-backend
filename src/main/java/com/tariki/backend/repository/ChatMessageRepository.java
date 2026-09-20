package com.tariki.backend.repository;

import com.tariki.backend.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChannelKeyAndIdLessThanOrderByIdDesc(String key, Long before, Pageable page);
    Optional<ChatMessage> findByChannelKeyAndSenderIdAndClientMessageId(String key, Long sender, UUID requestId);
    Optional<ChatMessage> findFirstByChannelKeyOrderByIdDesc(String key);
    long countByChannelKeyAndRecipientIdAndReadAtIsNull(String key, Long recipient);

    @Modifying
    @Query("update ChatMessage m set m.readAt = :now where m.channelKey = :key and m.recipientId = :recipient and m.id <= :through and m.readAt is null")
    int markRead(@Param("key") String key, @Param("recipient") Long recipient, @Param("through") Long through, @Param("now") Instant now);
}
