package com.sonofnos.nibssgateway.kyc;

/**
 * KYC identity verification boundary (BVN-style lookup). No real KYC
 * vendor is wired up - FakeKycProvider is the only implementation, a
 * deterministic check against a hardcoded set of "known" identifiers.
 */
public interface KycProvider {

    KycResult verify(String accountNumber, String identifier);
}
