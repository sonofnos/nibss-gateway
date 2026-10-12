package com.sonofnos.nibssgateway.ledger;

import java.util.UUID;

/**
 * Outward transfers and inward credits move money between a customer
 * account here and the "outside world" on the other side of NIBSS. The
 * NIBSS settlement suspense account stands in for that outside world
 * on this ledger's books: an outward transfer debits the customer and
 * credits this account; an inward credit debits this account and
 * credits the customer. Seeded once by V1 with this fixed id so both
 * sides of every posting always resolve to a real account row.
 */
public final class SettlementAccounts {

    public static final UUID NIBSS_SETTLEMENT_SUSPENSE = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private SettlementAccounts() {
    }
}
