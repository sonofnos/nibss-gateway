package com.sonofnos.nibssgateway.integration;

import com.sonofnos.nibssgateway.domain.Account;
import com.sonofnos.nibssgateway.repository.AccountRepository;
import com.sonofnos.nibssgateway.repository.InwardCreditRepository;
import com.sonofnos.nibssgateway.security.HmacSignatureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Proves inward-credit webhook idempotency under replay: the same
 * NIBSS reference delivered twice (or N times concurrently) results in
 * exactly one credit to the ledger, enforced by the unique constraint
 * on inward_credits.nibss_reference, not by a prior lookup.
 */
class InwardCreditIdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private InwardCreditRepository inwardCreditRepository;
    @Autowired
    private HmacSignatureService hmacSignatureService;

    private void postWebhook(String body, String signature) throws Exception {
        mockMvc.perform(post("/api/nip/webhooks/inward-credits")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-NIBSS-Signature", signature)
                .content(body));
    }

    @Test
    void replayedWebhookCreditsOnlyOnce() throws Exception {
        Account destination = accountRepository.save(new Account("4000000001", "12345678901", "RECEIVER", "000001",
                BigDecimal.ZERO));

        String body = "{\"nibssReference\":\"NIBSS-REF-REPLAY-1\",\"destinationAccountId\":\"" + destination.getId()
                + "\",\"sourceBankCode\":\"000002\",\"sourceAccountNumber\":\"9988776655\",\"amount\":750.00}";
        String signature = hmacSignatureService.sign(body);

        postWebhook(body, signature);
        postWebhook(body, signature);

        Account refreshed = accountRepository.findById(destination.getId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualTo(new BigDecimal("750.00"));
        assertThat(inwardCreditRepository.findByNibssReference("NIBSS-REF-REPLAY-1")).isPresent();
    }

    @Test
    void concurrentReplaysStillCreditOnlyOnce() throws InterruptedException {
        Account destination = accountRepository.save(new Account("4000000002", "12345678901", "RECEIVER 2", "000001",
                BigDecimal.ZERO));

        String body = "{\"nibssReference\":\"NIBSS-REF-CONCURRENT-1\",\"destinationAccountId\":\"" + destination.getId()
                + "\",\"sourceBankCode\":\"000002\",\"sourceAccountNumber\":\"9988776655\",\"amount\":100.00}";
        String signature = hmacSignatureService.sign(body);

        int concurrency = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(concurrency);
        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    postWebhook(body, signature);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        Account refreshed = accountRepository.findById(destination.getId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualTo(new BigDecimal("100.00"));
    }
}
