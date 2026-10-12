package com.sonofnos.nibssgateway.web;

import com.sonofnos.nibssgateway.domain.Account;
import com.sonofnos.nibssgateway.service.AccountService;
import com.sonofnos.nibssgateway.web.dto.OpenAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Minimal account onboarding, enough to create demo accounts to drive
 * the NIP endpoints manually or in tests. Not a real core-banking
 * ledger onboarding flow.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account open(@Valid @RequestBody OpenAccountRequest request) {
        return accountService.open(request.accountNumber(), request.bvn(), request.accountName(),
                request.bankCode(), request.openingBalance());
    }

    @GetMapping("/{id}")
    public Account get(@PathVariable UUID id) {
        return accountService.get(id);
    }
}
