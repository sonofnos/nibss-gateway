package com.sonofnos.nibssgateway.integration;

import com.sonofnos.nibssgateway.domain.Account;
import com.sonofnos.nibssgateway.domain.EntryType;
import com.sonofnos.nibssgateway.domain.LedgerEntry;
import com.sonofnos.nibssgateway.ledger.LedgerService;
import com.sonofnos.nibssgateway.repository.AccountRepository;
import com.sonofnos.nibssgateway.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the ledger invariant (sum(debit) == sum(credit)) holds under
 * concurrent transfers that run in both directions between the same
 * two accounts - reciprocal transfers are exactly the case that
 * deadlocks if locks aren't taken in a fixed order. LedgerService
 * locks both accounts by ascending id regardless of debit/credit
 * direction, so this should complete cleanly with no deadlock errors.
 */
class LedgerConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LedgerService ledgerService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void concurrentReciprocalTransfersStayBalanced() throws InterruptedException {
        Account accountA = accountRepository.save(new Account("1000000001", "11111111111", "ACCOUNT A", "000001",
                new BigDecimal("1000000.00")));
        Account accountB = accountRepository.save(new Account("1000000002", "22222222222", "ACCOUNT B", "000001",
                new BigDecimal("1000000.00")));

        int threadsPerDirection = 10;
        int totalThreads = threadsPerDirection * 2;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadsPerDirection; i++) {
            String ref = "CONC-A-TO-B-" + i;
            executor.submit(() -> {
                try {
                    ledgerService.post(accountA.getId(), accountB.getId(), new BigDecimal("10.00"), ref, "a to b");
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
            String ref2 = "CONC-B-TO-A-" + i;
            executor.submit(() -> {
                try {
                    ledgerService.post(accountB.getId(), accountA.getId(), new BigDecimal("10.00"), ref2, "b to a");
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(failures.get()).isZero();

        List<LedgerEntry> entriesA = ledgerEntryRepository.findByAccountId(accountA.getId());
        List<LedgerEntry> entriesB = ledgerEntryRepository.findByAccountId(accountB.getId());

        BigDecimal totalDebits = Stream.concat(entriesA.stream(), entriesB.stream())
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = Stream.concat(entriesA.stream(), entriesB.stream())
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalDebits).isEqualTo(totalCredits);
        assertThat(entriesA).hasSize(threadsPerDirection * 2);
        assertThat(entriesB).hasSize(threadsPerDirection * 2);

        // Equal volume both directions at equal amounts: net balance change is zero.
        Account refreshedA = accountRepository.findById(accountA.getId()).orElseThrow();
        Account refreshedB = accountRepository.findById(accountB.getId()).orElseThrow();
        assertThat(refreshedA.getBalance()).isEqualTo(new BigDecimal("1000000.00"));
        assertThat(refreshedB.getBalance()).isEqualTo(new BigDecimal("1000000.00"));
    }
}
