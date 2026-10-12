package com.sonofnos.nibssgateway.kyc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FakeKycProviderTest {

    private final FakeKycProvider provider = new FakeKycProvider();

    @Test
    void verifiesWellFormedIdentifier() {
        KycResult result = provider.verify("0123456789", "12345678901");
        assertThat(result.verified()).isTrue();
        assertThat(result.tier()).isEqualTo("TIER_2");
    }

    @Test
    void rejectsWrongLengthIdentifier() {
        KycResult result = provider.verify("0123456789", "123");
        assertThat(result.verified()).isFalse();
    }

    @Test
    void rejectsIdentifierStartingWithZero() {
        KycResult result = provider.verify("0123456789", "01234567890");
        assertThat(result.verified()).isFalse();
    }
}
