package com.tariki.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Component
public class ChatEncryption {
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public ChatEncryption(@Value("${app.chat.encryption-key:}") String encodedKey) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encodedKey); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("CHAT_ENCRYPTION_KEY doit etre une cle Base64 de 32 octets"); }
        if (bytes.length != 32) throw new IllegalStateException("CHAT_ENCRYPTION_KEY doit etre configuree (32 octets en Base64)");
        key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String text, String channel, Long sender, UUID messageId) {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce, channel, sender, messageId);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException ex) { throw new IllegalStateException("Chiffrement du message impossible", ex); }
    }

    public String decrypt(String envelope, String channel, Long sender, UUID messageId) {
        try {
            if (!envelope.startsWith("v1:")) throw new IllegalArgumentException();
            ByteBuffer bytes = ByteBuffer.wrap(Base64.getDecoder().decode(envelope.substring(3)));
            if (bytes.remaining() < 28) throw new IllegalArgumentException();
            byte[] nonce = new byte[12];
            bytes.get(nonce);
            byte[] encrypted = new byte[bytes.remaining()];
            bytes.get(encrypted);
            return new String(cipher(Cipher.DECRYPT_MODE, nonce, channel, sender, messageId).doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Message chiffre illisible ou altere", ex);
        }
    }

    private Cipher cipher(int mode, byte[] nonce, String channel, Long sender, UUID messageId) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        // Bind the ciphertext to its conversation, sender and logical message.
        cipher.updateAAD(("tariki.chat.v1|" + channel + "|" + sender + "|" + messageId).getBytes(StandardCharsets.UTF_8));
        return cipher;
    }
}
