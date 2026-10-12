package com.sonofnos.nibssgateway.service;

import com.sonofnos.nibssgateway.domain.Account;
import com.sonofnos.nibssgateway.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account open(String accountNumber, String bvn, String accountName, String bankCode, BigDecimal openingBalance) {
        return accountRepository.save(new Account(accountNumber, bvn, accountName, bankCode, openingBalance));
    }

    public Account get(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No account " + id));
    }
}
