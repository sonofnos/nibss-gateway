package com.sonofnos.nibssgateway.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void creditIncreasesBalance() {
        Account account = new Account("0123456789", "12345678901", "TEST", "000001", new BigDecimal("100.00"));
        account.credit(new BigDecimal("50.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    void debitDecreasesBalance() {
        Account account = new Account("0123456789", "12345678901", "TEST", "000001", new BigDecimal("100.00"));
        account.debit(new BigDecimal("40.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("60.00"));
    }

    @Test
    void debitBeyondBalanceThrows() {
        Account account = new Account("0123456789", "12345678901", "TEST", "000001", new BigDecimal("10.00"));
        assertThatThrownBy(() -> account.debit(new BigDecimal("10.01")))
                .isInstanceOf(InsufficientFundsException.class);
    }
}
