package com.sonofnos.nibssgateway.domain;

import com.sonofnos.nibssgateway.security.EncryptedStringConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue
    private UUID id;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "bvn")
    private String bvn;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * True only for the NIBSS settlement suspense account. A real
     * customer account can never go negative; the suspense account
     * stands in for the outside world on the other side of every NIP
     * posting and is allowed to, exactly like a real interbank clearing
     * account.
     */
    @Column(name = "allow_negative", nullable = false)
    private boolean allowNegative = false;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Account() {
    }

    public Account(String accountNumber, String bvn, String accountName, String bankCode, BigDecimal openingBalance) {
        this.accountNumber = accountNumber;
        this.bvn = bvn;
        this.accountName = accountName;
        this.bankCode = bankCode;
        this.balance = openingBalance;
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getBvn() {
        return bvn;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getBankCode() {
        return bankCode;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (!allowNegative && this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id, amount, this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void markAllowNegative() {
        this.allowNegative = true;
    }

    public Long getVersion() {
        return version;
    }
}
