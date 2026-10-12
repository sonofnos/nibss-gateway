package com.sonofnos.nibssgateway.security;

import com.sonofnos.nibssgateway.config.EncryptionProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Field-level AES-GCM encryption for PII (account numbers, BVN-equivalent
 * identifiers) at rest.
 *
 * <p>Ciphertext is stored as {@code <keyVersion>:<base64 iv>:<base64
 * ciphertext+tag>}, so the key version used to encrypt a field travels
 * with the field itself rather than living in a separate column. That is
 * what makes key rotation possible without a schema change or a big-bang
 * re-encryption: decrypt() looks up whichever version prefixes the stored
 * value, so old rows keep working under a retired key while new writes
 * use the active one.
 *
 * <p><b>Key rotation procedure</b> (only one version, "v1", is live in
 * this project, but this is how a second version would be introduced):
 * <ol>
 *   <li>Add the new key under a new version, e.g. app.encryption.keys.v2,
 *   alongside the existing v1 key - both must be configured at once.</li>
 *   <li>Set app.encryption.active-key-version=v2. New writes now encrypt
 *   under v2; existing v1 ciphertext still decrypts correctly because v1
 *   stays in the key map.</li>
 *   <li>Optionally run a background job that reads and re-saves every
 *   encrypted field (decrypt under its stored version, re-encrypt under
 *   the active version) to migrate old rows off v1.</li>
 *   <li>Once no row references v1 any more, remove it from the key map.</li>
 * </ol>
 */
@Component
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private static volatile EncryptionService instance;

    private final EncryptionProperties properties;
    private final Map<String, SecretKeySpec> keysByVersion = new HashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(EncryptionProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        properties.getKeys().forEach((version, base64Key) -> {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            keysByVersion.put(version, new SecretKeySpec(keyBytes, "AES"));
        });
        if (!keysByVersion.containsKey(properties.getActiveKeyVersion())) {
            throw new IllegalStateException(
                    "No encryption key configured for active key version " + properties.getActiveKeyVersion());
        }
        instance = this;
    }

    /**
     * Converters are plain classes, not Spring beans picked up by
     * constructor injection, so they reach this singleton through a
     * static reference set once at startup. There is exactly one
     * EncryptionService bean in the application context, so this is a
     * safe substitute for injection here, not a general-purpose pattern.
     */
    public static EncryptionService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EncryptionService not initialized yet");
        }
        return instance;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            String version = properties.getActiveKeyVersion();
            SecretKeySpec key = keysByVersion.get(version);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return version + ":" + Base64.getEncoder().encodeToString(iv)
                    + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Field encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            String[] parts = stored.split(":", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Malformed encrypted field value");
            }
            String version = parts[0];
            SecretKeySpec key = keysByVersion.get(version);
            if (key == null) {
                throw new IllegalStateException("No key configured for version " + version
                        + " - add it to app.encryption.keys to decrypt rows written under it");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Field decryption failed", e);
        }
    }
}
