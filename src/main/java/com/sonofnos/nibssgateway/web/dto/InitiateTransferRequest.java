package com.sonofnos.nibssgateway.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiateTransferRequest(
        @NotBlank String clientReference,
        @NotNull UUID sourceAccountId,
        @NotBlank String destinationBankCode,
        @NotBlank String destinationAccountNumber,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String narration) {
}
