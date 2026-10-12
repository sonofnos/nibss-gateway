package com.sonofnos.nibssgateway.web.dto;

import jakarta.validation.constraints.NotBlank;

public record NameEnquiryRequest(@NotBlank String destinationBankCode, @NotBlank String accountNumber) {
}
