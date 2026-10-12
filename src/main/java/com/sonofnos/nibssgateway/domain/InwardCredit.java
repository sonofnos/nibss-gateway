package com.sonofnos.nibssgateway.domain;

import com.sonofnos.nibssgateway.security.EncryptedStringConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a NIBSS credit notification for an incoming interbank
 * transfer. Idempotent on nibssReference via a unique DB constraint -
 * the same NIBSS reference replayed by a retried webhook delivery hits
 * that constraint on the second insert rather than crediting twice.
 */
@Entity
@Table(name = "inward_credits")
public class InwardCredit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "nibss_reference", nullable = false, unique = true)
    private String nibssReference;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(name = "source_bank_code", nullable = false)
    private String sourceBankCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "source_account_number", nullable = false)
    private String sourceAccountNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InwardCreditStatus status = InwardCreditStatus.CREDITED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected InwardCredit() {
    }

    public InwardCredit(String nibssReference, UUID destinationAccountId, String sourceBankCode,
                         String sourceAccountNumber, BigDecimal amount) {
        this.nibssReference = nibssReference;
        this.destinationAccountId = destinationAccountId;
        this.sourceBankCode = sourceBankCode;
        this.sourceAccountNumber = sourceAccountNumber;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public String getNibssReference() {
        return nibssReference;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public InwardCreditStatus getStatus() {
        return status;
    }

    public void markReversed() {
        this.status = InwardCreditStatus.REVERSED;
    }
}
