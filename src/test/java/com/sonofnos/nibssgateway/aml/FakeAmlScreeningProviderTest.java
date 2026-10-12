package com.sonofnos.nibssgateway.aml;

import com.sonofnos.nibssgateway.config.AmlProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FakeAmlScreeningProviderTest {

    private final AmlProperties properties = new AmlProperties();
    private final FakeAmlScreeningProvider provider = new FakeAmlScreeningProvider(properties);

    FakeAmlScreeningProviderTest() {
        properties.setFlagThreshold(new BigDecimal("1000000.00"));
        properties.setWatchlistAccountNumbers(List.of("0000000001"));
    }

    @Test
    void passesOrdinaryTransfer() {
        ScreeningResult result = provider.screen("1122334455", new BigDecimal("5000.00"));
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void flagsAmountAtOrAboveThreshold() {
        ScreeningResult result = provider.screen("1122334455", new BigDecimal("1000000.00"));
        assertThat(result.isFlagged()).isTrue();
    }

    @Test
    void flagsWatchlistedAccountRegardlessOfAmount() {
        ScreeningResult result = provider.screen("0000000001", new BigDecimal("1.00"));
        assertThat(result.isFlagged()).isTrue();
    }
}
