package com.sonofnos.nibssgateway.aml;

import com.sonofnos.nibssgateway.config.AmlProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Deterministic fake AML/fraud screen. Flags a transfer when either:
 * <ul>
 *   <li>the amount is at or above app.aml.flag-threshold, or</li>
 *   <li>the destination account number is on a hardcoded test
 *   watchlist (app.aml.watchlist-account-numbers).</li>
 * </ul>
 * This is not a real sanctions, PEP or fraud-model screen. It exists to
 * prove the PENDING_REVIEW branch of OutwardTransferService works.
 */
@Component
public class FakeAmlScreeningProvider implements AmlScreeningProvider {

    private final AmlProperties properties;

    public FakeAmlScreeningProvider(AmlProperties properties) {
        this.properties = properties;
    }

    @Override
    public ScreeningResult screen(String destinationAccountNumber, BigDecimal amount) {
        if (properties.getWatchlistAccountNumbers().contains(destinationAccountNumber)) {
            return new ScreeningResult(ScreeningDecision.FLAGGED,
                    "Destination account is on the simulated watchlist");
        }
        if (amount.compareTo(properties.getFlagThreshold()) >= 0) {
            return new ScreeningResult(ScreeningDecision.FLAGGED,
                    "Amount " + amount + " meets or exceeds flag threshold " + properties.getFlagThreshold());
        }
        return new ScreeningResult(ScreeningDecision.PASS, null);
    }
}
