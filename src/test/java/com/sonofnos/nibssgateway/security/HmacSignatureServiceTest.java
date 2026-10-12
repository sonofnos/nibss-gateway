package com.sonofnos.nibssgateway.security;

import com.sonofnos.nibssgateway.config.WebhookProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignatureServiceTest {

    private final WebhookProperties properties = new WebhookProperties();
    private final HmacSignatureService service;

    HmacSignatureServiceTest() {
        properties.setHmacSecret("test-secret");
        service = new HmacSignatureService(properties);
    }

    @Test
    void validSignatureIsAccepted() {
        String body = "{\"nibssReference\":\"abc123\"}";
        String signature = service.sign(body);

        assertThat(service.isValid(body, signature)).isTrue();
    }

    @Test
    void tamperedBodyIsRejected() {
        String body = "{\"amount\":100}";
        String signature = service.sign(body);

        assertThat(service.isValid("{\"amount\":999}", signature)).isFalse();
    }

    @Test
    void wrongSecretIsRejected() {
        String body = "{\"amount\":100}";
        String signature = service.sign(body, "a-different-secret");

        assertThat(service.isValid(body, signature)).isFalse();
    }

    @Test
    void blankSignatureIsRejected() {
        assertThat(service.isValid("{}", "")).isFalse();
        assertThat(service.isValid("{}", null)).isFalse();
    }
}
