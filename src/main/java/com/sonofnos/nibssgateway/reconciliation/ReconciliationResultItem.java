package com.sonofnos.nibssgateway.reconciliation;

import java.math.BigDecimal;

public record ReconciliationResultItem(String reference, ReconciliationStatus status,
                                        BigDecimal ledgerAmount, BigDecimal settlementAmount, String detail) {
}
