package com.sonofnos.nibssgateway.ledger;

import com.sonofnos.nibssgateway.security.EncryptionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeds the NIBSS settlement suspense account on startup if it does
 * not already exist. This cannot be a Flyway migration: account_number
 * goes through the same AES-GCM field encryption as every other
 * account (EncryptedStringConverter), and the active encryption key is
 * only known to the running application, not to a SQL script.
 */
@Component
public class SuspenseAccountSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionService encryptionService;

    public SuspenseAccountSeeder(JdbcTemplate jdbcTemplate, EncryptionService encryptionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionService = encryptionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.update(
                "insert into accounts (id, account_number, account_name, bank_code, balance, allow_negative) "
                        + "values (?, ?, ?, ?, 0, true) on conflict (id) do nothing",
                SettlementAccounts.NIBSS_SETTLEMENT_SUSPENSE,
                encryptionService.encrypt("NIBSS-SUSPENSE"),
                "NIBSS Settlement Suspense",
                "999999");
    }
}
