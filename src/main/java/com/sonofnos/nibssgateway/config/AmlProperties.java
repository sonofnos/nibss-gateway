package com.sonofnos.nibssgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thresholds for the fake AML screening provider. See
 * aml.FakeAmlScreeningProvider for what this actually checks - a
 * deterministic amount threshold and a hardcoded watchlist, not a real
 * sanctions or PEP list.
 */
@ConfigurationProperties(prefix = "app.aml")
public class AmlProperties {

    private BigDecimal flagThreshold = new BigDecimal("1000000.00");
    private List<String> watchlistAccountNumbers = List.of("0000000001");

    public BigDecimal getFlagThreshold() {
        return flagThreshold;
    }

    public void setFlagThreshold(BigDecimal flagThreshold) {
        this.flagThreshold = flagThreshold;
    }

    public List<String> getWatchlistAccountNumbers() {
        return watchlistAccountNumbers;
    }

    public void setWatchlistAccountNumbers(List<String> watchlistAccountNumbers) {
        this.watchlistAccountNumbers = watchlistAccountNumbers;
    }
}
