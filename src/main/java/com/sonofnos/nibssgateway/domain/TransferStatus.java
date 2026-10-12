package com.sonofnos.nibssgateway.domain;

public enum TransferStatus {
    /** Accepted, not yet screened. */
    INITIATED,
    /** Flagged by AML screening, held for manual review before it can proceed. */
    PENDING_REVIEW,
    /** Passed screening, source account debited, NIP transfer sent to NIBSS. */
    PROCESSING,
    /** NIBSS did not return a definitive result in time; requery is in progress or exhausted. */
    REQUERY_PENDING,
    /** NIBSS confirmed the transfer landed. */
    SUCCESSFUL,
    /** NIBSS confirmed the transfer did not land; source account was never debited or has been reversed. */
    FAILED,
    /** A reversal was posted against an already-successful transfer. */
    REVERSED
}
