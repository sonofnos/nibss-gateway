package com.sonofnos.nibssgateway.security;

import com.sonofnos.nibssgateway.config.WebhookProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HMAC-SHA256 verification for inbound NIBSS-style webhooks (inward
 * credit, reversal notifications). Same pattern as
 * payments-collections-service's WebhookSignatureService: sign the raw
 * request body, compare in constant time.
 */
@Service
public class HmacSignatureService {

    private final WebhookProperties properties;

    public HmacSignatureService(WebhookProperties properties) {
        this.properties = properties;
    }

    public String sign(String rawBody) {
        return sign(rawBody, properties.getHmacSecret());
    }

    public String sign(String rawBody, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    /**
     * Verifies a signature against the raw body bytes exactly as
     * received - parsing to a DTO first and re-serializing to verify
     * would let field-order/whitespace differences silently break real
     * signatures. Uses MessageDigest.isEqual for a constant-time
     * comparison, avoiding a timing side-channel.
     */
    public boolean isValid(String rawBody, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        String expected = sign(rawBody);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                providedSignature.trim().getBytes(StandardCharsets.UTF_8));
    }
}
