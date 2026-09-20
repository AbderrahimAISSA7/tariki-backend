package com.tariki.backend;

import com.tariki.backend.security.ChatEncryption;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class ChatEncryptionTest {
    private final ChatEncryption encryption = new ChatEncryption(Base64.getEncoder().encodeToString(new byte[32]));

    @Test void authenticatedEncryptionUsesFreshNonceAndRejectsTampering() {
        UUID id = UUID.randomUUID();
        String a = encryption.encrypt("Texte prive", "channel", 1L, id);
        String b = encryption.encrypt("Texte prive", "channel", 1L, id);
        assertThat(a).isNotEqualTo(b).doesNotContain("Texte prive");
        assertThat(encryption.decrypt(a, "channel", 1L, id)).isEqualTo("Texte prive");
        byte[] altered = Base64.getDecoder().decode(a.substring(3));
        altered[altered.length - 1] ^= 1;
        assertThatThrownBy(() -> encryption.decrypt("v1:" + Base64.getEncoder().encodeToString(altered), "channel", 1L, id)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> encryption.decrypt(a, "other", 1L, id)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> encryption.decrypt(a, "channel", 2L, id)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> encryption.decrypt(a, "channel", 1L, UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
    }

    @Test void missingInvalidAndWrongKeysNeverFallBackToPlaintext() {
        assertThatThrownBy(() -> new ChatEncryption("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ChatEncryption("not-base64")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ChatEncryption(Base64.getEncoder().encodeToString(new byte[16]))).isInstanceOf(IllegalStateException.class);
        byte[] other = new byte[32]; other[0] = 1;
        ChatEncryption wrong = new ChatEncryption(Base64.getEncoder().encodeToString(other));
        UUID id = UUID.randomUUID();
        String encrypted = encryption.encrypt("Texte prive", "channel", 1L, id);
        assertThatThrownBy(() -> wrong.decrypt(encrypted, "channel", 1L, id)).isInstanceOf(IllegalStateException.class);
    }

    @Test void defaultDebugRepresentationsDoNotExposeMessageContent() {
        String privateText = "Texte a ne pas journaliser";
        UUID id = UUID.randomUUID();
        assertThat(new com.tariki.backend.dto.ChatDTO.Send(privateText, id).toString()).doesNotContain(privateText);
        assertThat(new com.tariki.backend.dto.ChatDTO.Message(1L, id, 2L, privateText, java.time.Instant.now(), null).toString()).doesNotContain(privateText);
    }
}
