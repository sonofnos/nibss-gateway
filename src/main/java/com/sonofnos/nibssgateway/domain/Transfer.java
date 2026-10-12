package com.sonofnos.nibssgateway.domain;

import com.sonofnos.nibssgateway.security.EncryptedStringConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An outward NIP transfer: a debit on the source account here, paired
 * with an interbank transfer to a destination bank over NIBSS NIP.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue
    private UUID id;

    /** Client-supplied idempotency key. Unique DB constraint, not check-then-act. */
    @Column(name = "client_reference", nullable = false, unique = true)
    private String clientReference;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_bank_code", nullable = false)
    private String destinationBankCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "destination_account_number", nullable = false)
    private String destinationAccountNumber;

    @Column(name = "destination_account_name")
    private String destinationAccountName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "narration")
    private String narration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransferStatus status = TransferStatus.INITIATED;

    /** NIBSS NIP session id: 6-digit institution code + yyMMddHHmmss + 12 unique digits. */
    @Column(name = "nibss_session_id")
    private String nibssSessionId;

    @Column(name = "requery_count", nullable = false)
    private int requeryCount = 0;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Transfer() {
    }

    public Transfer(String clientReference, UUID sourceAccountId, String destinationBankCode,
                     String destinationAccountNumber, String destinationAccountName,
                     BigDecimal amount, String narration) {
        this.clientReference = clientReference;
        this.sourceAccountId = sourceAccountId;
        this.destinationBankCode = destinationBankCode;
        this.destinationAccountNumber = destinationAccountNumber;
        this.destinationAccountName = destinationAccountName;
        this.amount = amount;
        this.narration = narration;
    }

    public void moveTo(TransferStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void incrementRequery() {
        this.requeryCount++;
        this.updatedAt = Instant.now();
    }

    public void assignSessionId(String nibssSessionId) {
        this.nibssSessionId = nibssSessionId;
    }

    public UUID getId() {
        return id;
    }

    public String getClientReference() {
        return clientReference;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public String getDestinationBankCode() {
        return destinationBankCode;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public String getDestinationAccountName() {
        return destinationAccountName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNarration() {
        return narration;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getNibssSessionId() {
        return nibssSessionId;
    }

    public int getRequeryCount() {
        return requeryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
