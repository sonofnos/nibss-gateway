package com.sonofnos.nibssgateway.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A NIBSS-initiated reversal of a previously successful outward
 * transfer. Idempotent on nibssReference via a unique constraint, same
 * pattern as InwardCredit. Processing a reversal never deletes the
 * original ledger entries; it posts a correcting double-entry that
 * credits the source account back and debits the settlement suspense
 * account, so the ledger history stays a true record of what happened.
 */
@Entity
@Table(name = "reversals")
public class Reversal {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "nibss_reference", nullable = false, unique = true)
    private String nibssReference;

    @Column(name = "original_transfer_id", nullable = false)
    private UUID originalTransferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReversalReason reason;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Reversal() {
    }

    public Reversal(String nibssReference, UUID originalTransferId, ReversalReason reason, BigDecimal amount) {
        this.nibssReference = nibssReference;
        this.originalTransferId = originalTransferId;
        this.reason = reason;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public String getNibssReference() {
        return nibssReference;
    }

    public UUID getOriginalTransferId() {
        return originalTransferId;
    }

    public ReversalReason getReason() {
        return reason;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
