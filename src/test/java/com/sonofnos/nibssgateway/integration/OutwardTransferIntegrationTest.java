package com.sonofnos.nibssgateway.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonofnos.nibssgateway.domain.Account;
import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.domain.TransferStatus;
import com.sonofnos.nibssgateway.nip.FakeNibssNipClient;
import com.sonofnos.nibssgateway.repository.AccountRepository;
import com.sonofnos.nibssgateway.repository.TransferRepository;
import com.sonofnos.nibssgateway.web.dto.InitiateTransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP-stack coverage of outward transfers: AML pending-review
 * branch, the NIBSS timeout -> requery -> success/failure paths, and
 * idempotent replay of the same client reference.
 */
class OutwardTransferIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransferRepository transferRepository;

    private Account openAccount(String accountNumber, BigDecimal balance) {
        return accountRepository.save(new Account(accountNumber, "12345678901", "TEST CUSTOMER", "000099", balance));
    }

    private Transfer initiate(InitiateTransferRequest request) throws Exception {
        String json = mockMvc.perform(post("/api/nip/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, Transfer.class);
    }

    @Test
    void ordinaryTransferSucceeds() throws Exception {
        Account source = openAccount("3000000001", new BigDecimal("5000.00"));
        InitiateTransferRequest request = new InitiateTransferRequest("ORD-1", source.getId(), "000001",
                "0123456789", new BigDecimal("1000.00"), "rent");

        Transfer result = initiate(request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.SUCCESSFUL);
        Account refreshed = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualTo(new BigDecimal("4000.00"));
    }

    @Test
    void timeoutResolvesToSuccessfulViaRequery() throws Exception {
        Account source = openAccount("3000000002", new BigDecimal("5000.00"));
        InitiateTransferRequest request = new InitiateTransferRequest("TIMEOUT-OK-1", source.getId(), "000001",
                FakeNibssNipClient.TIMEOUT_THEN_SUCCESS_ACCOUNT, new BigDecimal("200.00"), "timeout test");

        Transfer result = initiate(request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.SUCCESSFUL);
        assertThat(result.getRequeryCount()).isEqualTo(1);
    }

    @Test
    void timeoutResolvesToFailureAndReversesTheDebit() throws Exception {
        Account source = openAccount("3000000003", new BigDecimal("5000.00"));
        InitiateTransferRequest request = new InitiateTransferRequest("TIMEOUT-FAIL-1", source.getId(), "000001",
                FakeNibssNipClient.TIMEOUT_THEN_FAILURE_ACCOUNT, new BigDecimal("200.00"), "timeout test");

        Transfer result = initiate(request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.FAILED);
        Account refreshed = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    void watchlistedDestinationGoesToPendingReviewWithoutDebiting() throws Exception {
        Account source = openAccount("3000000004", new BigDecimal("5000.00"));
        InitiateTransferRequest request = new InitiateTransferRequest("WATCHLIST-1", source.getId(), "000001",
                "0000000001", new BigDecimal("50.00"), "flagged");

        Transfer result = initiate(request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.PENDING_REVIEW);
        Account refreshed = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    void replayingTheSameClientReferenceDoesNotDebitTwice() throws Exception {
        Account source = openAccount("3000000005", new BigDecimal("5000.00"));
        InitiateTransferRequest request = new InitiateTransferRequest("REPLAY-1", source.getId(), "000001",
                "1122334455", new BigDecimal("300.00"), "replay test");

        Transfer first = initiate(request);
        Transfer second = initiate(request);

        assertThat(first.getId()).isEqualTo(second.getId());
        Account refreshed = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(refreshed.getBalance()).isEqualTo(new BigDecimal("4700.00"));
        assertThat(transferRepository.findAllByOrderByCreatedAtAsc()
                .stream().filter(t -> t.getClientReference().equals("REPLAY-1")).count()).isEqualTo(1);
    }
}
