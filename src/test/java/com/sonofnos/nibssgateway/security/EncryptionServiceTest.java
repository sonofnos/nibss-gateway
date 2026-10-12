package com.sonofnos.nibssgateway.security;

import com.sonofnos.nibssgateway.config.EncryptionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private static final String KEY_V1 = "dGVzdC1vbmx5LWtleS1tdXN0LWJlLTMyLWJ5dGVzISE=";
    private static final String KEY_V2 = "c2Vjb25kLXRlc3Qta2V5LW11c3QtYmUtMzItYnl0ZXM=";

    private EncryptionService service;

    @BeforeEach
    void setUp() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setKeys(Map.of("v1", KEY_V1, "v2", KEY_V2));
        properties.setActiveKeyVersion("v1");
        service = new EncryptionService(properties);
        service.init();
    }

    @Test
    void encryptsAndDecryptsRoundTrip() {
        String plaintext = "0123456789";
        String ciphertext = service.encrypt(plaintext);

        assertThat(ciphertext).startsWith("v1:").doesNotContain(plaintext);
        assertThat(service.decrypt(ciphertext)).isEqualTo(plaintext);
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        String plaintext = "22222222222";
        assertThat(service.encrypt(plaintext)).isNotEqualTo(service.encrypt(plaintext));
    }

    @Test
    void nullPassesThrough() {
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void oldKeyVersionStillDecryptsAfterRotation() {
        String ciphertextUnderV1 = service.encrypt("9999999999");

        EncryptionProperties rotated = new EncryptionProperties();
        rotated.setKeys(Map.of("v1", KEY_V1, "v2", KEY_V2));
        rotated.setActiveKeyVersion("v2");
        EncryptionService rotatedService = new EncryptionService(rotated);
        rotatedService.init();

        // New writes go out under v2...
        assertThat(rotatedService.encrypt("9999999999")).startsWith("v2:");
        // ...but a row still carrying v1 ciphertext keeps decrypting correctly.
        assertThat(rotatedService.decrypt(ciphertextUnderV1)).isEqualTo("9999999999");
    }

    @Test
    void unknownKeyVersionFailsClearly() {
        String tampered = "v99:AAAAAAAAAAAAAAAAAA==:AAAAAAAAAAAAAAAAAA==";
        assertThatThrownBy(() -> service.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }
}
