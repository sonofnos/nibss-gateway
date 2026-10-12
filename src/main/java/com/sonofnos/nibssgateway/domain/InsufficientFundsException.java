package com.sonofnos.nibssgateway.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID accountId, BigDecimal requested, BigDecimal available) {
        super("Account " + accountId + " has insufficient funds: requested " + requested + ", available " + available);
    }
}
