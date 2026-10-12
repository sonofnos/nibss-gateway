package com.sonofnos.nibssgateway.kyc;

public record KycResult(boolean verified, String tier, String reason) {
}
