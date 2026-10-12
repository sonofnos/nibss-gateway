package com.sonofnos.nibssgateway.aml;

import java.math.BigDecimal;

/**
 * Transaction/AML screening boundary. Every outward transfer must pass
 * through an implementation of this before it is allowed to proceed. No
 * real AML vendor is wired up anywhere in this repo - the only
 * implementation is FakeAmlScreeningProvider, a deterministic
 * threshold-and-watchlist check, never represented as a real sanctions
 * or PEP screen.
 */
public interface AmlScreeningProvider {

    ScreeningResult screen(String destinationAccountNumber, BigDecimal amount);
}
