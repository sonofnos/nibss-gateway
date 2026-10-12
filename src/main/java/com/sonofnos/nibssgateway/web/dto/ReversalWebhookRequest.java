package com.sonofnos.nibssgateway.web.dto;

import com.sonofnos.nibssgateway.domain.ReversalReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReversalWebhookRequest(
        @NotBlank String nibssReference,
        @NotBlank String nibssSessionId,
        @NotNull ReversalReason reason) {
}
