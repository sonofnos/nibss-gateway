package com.sonofnos.nibssgateway.nip;

/**
 * A small slice of NIBSS's real NIP response code set, enough to drive
 * the requery-never-retry decision. "00" is the only success code; a
 * handful of codes (listed here) prove nothing happened on the NIBSS
 * side and are safe to treat as a definite failure. Everything else -
 * including a transport timeout, where there was no response code at
 * all - is UNKNOWN, and UNKNOWN is resolved only by TSQ, never by
 * resending the transfer.
 */
public enum NipResponseCode {
    APPROVED,
    DEFINITE_FAILURE,
    UNKNOWN
}
