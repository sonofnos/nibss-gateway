package com.sonofnos.nibssgateway.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InwardCreditWebhookRequest(
        @NotBlank String nibssReference,
        @NotNull UUID destinationAccountId,
        @NotBlank String sourceBankCode,
        @NotBlank String sourceAccountNumber,
        @NotNull BigDecimal amount) {
}
