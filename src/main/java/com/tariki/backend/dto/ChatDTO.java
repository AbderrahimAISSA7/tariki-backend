package com.tariki.backend.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatDTO {
    private ChatDTO() { }
    public record Conversation(String key, String kind, Long peerId, String peerName, String context,
                               Long livraisonId, String reference, String statut, Instant lastMessageAt, long unread) { }
    public record Message(Long id, UUID clientMessageId, Long senderId, String content, Instant sentAt, Instant readAt) {
        @Override public String toString() { return "Message[id=" + id + ", content=REDACTED]"; }
    }
    public record History(List<Message> messages, boolean hasMore, Long nextBefore) { }
    public record Send(@NotBlank(message = "Le message est vide") @Size(max = 4000, message = "Message limite a 4000 caracteres") String content,
                       @NotNull UUID clientMessageId) {
        @Override public String toString() { return "Send[content=REDACTED]"; }
    }
    public record Read(@NotNull @Positive Long throughId) { }
}
