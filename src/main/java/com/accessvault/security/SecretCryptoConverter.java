package com.accessvault.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class SecretCryptoConverter implements AttributeConverter<String, String> {
    private static final String ALG = "AES";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;    // 16 bytes tag
    private static final int IV_BYTES = 12;         // 96-bit IV

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public SecretCryptoConverter() {
        // Read Base64 key from ENV: ENC_KEY (must be 32 bytes when decoded for AES-256)
        String b64 = System.getenv("ENC_KEY");
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("ENC_KEY env var is required for secret encryption");
        }
        byte[] raw = Base64.getDecoder().decode(b64);
        if (raw.length != 32) {
            throw new IllegalStateException("ENC_KEY must decode to 32 bytes (AES-256)");
        }
        this.key = new SecretKeySpec(raw, ALG);
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // store as base64(iv):base64(ciphertext)
            return Base64.getEncoder().encodeToString(iv) + ":" +
                   Base64.getEncoder().encodeToString(ct);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String column) {
        if (column == null) return null;
        try {
            // If old plaintext still exists (pre-migration), just return it (so app won’t break).
            // We'll migrate it to encrypted form with a one-time runner below.
            if (!column.contains(":")) return column;

            String[] parts = column.split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ct = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
