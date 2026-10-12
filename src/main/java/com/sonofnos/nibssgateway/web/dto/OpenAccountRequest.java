package com.sonofnos.nibssgateway.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OpenAccountRequest(
        @NotBlank String accountNumber,
        String bvn,
        @NotBlank String accountName,
        @NotBlank String bankCode,
        @NotNull BigDecimal openingBalance) {
}
