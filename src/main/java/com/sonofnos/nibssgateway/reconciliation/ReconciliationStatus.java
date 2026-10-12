package com.sonofnos.nibssgateway.reconciliation;

public enum ReconciliationStatus {
    /** Ledger transfer and settlement file record agree on amount. No action. */
    MATCHED,
    /** Same reference on both sides, but the amounts disagree. Flagged for manual review; audit-logged. */
    AMOUNT_MISMATCH,
    /** Settlement file has this reference, but there is no matching transfer in this gateway's own ledger. Possible unrecorded or fraudulent settlement; audit-logged as an orphan. */
    MISSING_IN_LEDGER,
    /** This gateway recorded a transfer, but the settlement file (for the run's date) has no matching entry. Left pending - NIBSS may settle it on a later cycle - and is a candidate for a standalone TSQ. */
    MISSING_IN_SETTLEMENT
}
