package com.sonofnos.nibssgateway.reconciliation;

import java.math.BigDecimal;

/** One row of a daily NIBSS settlement file: reference,amount. */
public record SettlementRecord(String reference, BigDecimal amount) {
}
