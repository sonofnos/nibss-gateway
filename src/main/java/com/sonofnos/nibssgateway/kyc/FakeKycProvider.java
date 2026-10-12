package com.sonofnos.nibssgateway.kyc;

import org.springframework.stereotype.Component;

/**
 * Deterministic fake KYC check: an 11-digit identifier (same shape as a
 * Nigerian BVN) that does not start with "0" is treated as verified at
 * tier 2. This is simulated, not a real BVN/NIN lookup against any
 * vendor or NIBSS BVN service.
 */
@Component
public class FakeKycProvider implements KycProvider {

    @Override
    public KycResult verify(String accountNumber, String identifier) {
        if (identifier == null || !identifier.matches("\\d{11}")) {
            return new KycResult(false, null, "Identifier is not an 11-digit number (simulated BVN format check)");
        }
        if (identifier.startsWith("0")) {
            return new KycResult(false, null, "Simulated KYC provider could not verify this identifier");
        }
        return new KycResult(true, "TIER_2", null);
    }
}
