package com.sonofnos.nibssgateway.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonofnos.nibssgateway.domain.Account;
import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.domain.TransferStatus;
import com.sonofnos.nibssgateway.repository.AccountRepository;
import com.sonofnos.nibssgateway.repository.TransferRepository;
import com.sonofnos.nibssgateway.security.HmacSignatureService;
import com.sonofnos.nibssgateway.web.dto.InitiateTransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * A NIBSS-initiated reversal of a successful outward transfer: posts a
 * correcting double-entry (credits the source account back) rather
 * than deleting the original debit, and is idempotent on its own
 * nibssReference.
 */
class ReversalIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransferRepository transferRepository;
    @Autowired
    private HmacSignatureService hmacSignatureService;

    @Test
    void reversalCreditsSourceBackAndMarksTransferReversed() throws Exception {
        Account source = accountRepository.save(new Account("5000000001", "12345678901", "PAYER", "000099",
                new BigDecimal("1000.00")));
        InitiateTransferRequest transferRequest = new InitiateTransferRequest("REV-ORIGINAL-1", source.getId(),
                "000001", "0123456789", new BigDecimal("400.00"), "will be reversed");

        String transferJson = mockMvc.perform(post("/api/nip/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andReturn().getResponse().getContentAsString();
        Transfer original = objectMapper.readValue(transferJson, Transfer.class);
        assertThat(original.getStatus()).isEqualTo(TransferStatus.SUCCESSFUL);

        String body = "{\"nibssReference\":\"REV-NOTICE-1\",\"nibssSessionId\":\"" + original.getNibssSessionId()
                + "\",\"reason\":\"DESTINATION_ACCOUNT_NOT_FOUND\"}";
        String signature = hmacSignatureService.sign(body);

        mockMvc.perform(post("/api/nip/webhooks/reversals")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-NIBSS-Signature", signature)
                .content(body));

        Account refreshedSource = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(refreshedSource.getBalance()).isEqualTo(new BigDecimal("1000.00"));

        Transfer refreshedTransfer = transferRepository.findById(original.getId()).orElseThrow();
        assertThat(refreshedTransfer.getStatus()).isEqualTo(TransferStatus.REVERSED);
    }
}
