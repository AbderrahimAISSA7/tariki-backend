package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "chat_history_idx", columnList = "channel_key,id"),
        @Index(name = "chat_unread_idx", columnList = "recipient_id,channel_key,read_at")
}, uniqueConstraints = @UniqueConstraint(name = "chat_send_once", columnNames = {"channel_key", "sender_id", "client_message_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String channelKey;
    @Column(nullable = false)
    private Long senderId;
    @Column(nullable = false)
    private Long recipientId;
    @Column(nullable = false)
    private UUID clientMessageId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedBody;
    @Column(nullable = false)
    private Instant sentAt;
    private Instant readAt;
}
