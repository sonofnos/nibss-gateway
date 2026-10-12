package com.sonofnos.nibssgateway.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonofnos.nibssgateway.domain.Account;
import com.sonofnos.nibssgateway.reconciliation.ReconciliationResultItem;
import com.sonofnos.nibssgateway.reconciliation.ReconciliationStatus;
import com.sonofnos.nibssgateway.repository.AccountRepository;
import com.sonofnos.nibssgateway.web.dto.InitiateTransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Drives the real daily reconciliation endpoint end to end and checks
 * that all four categories are produced from a realistic settlement
 * file: one ledger transfer that matches, one that mismatches on
 * amount, one the file never mentions, and one the file mentions that
 * the ledger never recorded.
 */
class ReconciliationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void reconciliationRunClassifiesAllFourCategories() throws Exception {
        Account source = accountRepository.save(new Account("6000000001", "12345678901", "RECON PAYER", "000099",
                new BigDecimal("10000.00")));

        initiate(source, "RECON-MATCHED", "0123456789", new BigDecimal("500.00"));
        initiate(source, "RECON-MISMATCH", "1122334455", new BigDecimal("300.00"));
        initiate(source, "RECON-MISSING-SETTLEMENT", "9988776655", new BigDecimal("150.00"));

        String csv = "reference,amount\n"
                + "RECON-MATCHED,500.00\n"
                + "RECON-MISMATCH,299.00\n"
                + "RECON-ORPHAN,75.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "settlement.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        String json = mockMvc.perform(multipart("/api/reconciliation/runs").file(file))
                .andReturn().getResponse().getContentAsString();
        List<ReconciliationResultItem> results = objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ReconciliationResultItem.class));

        assertThat(results).extracting(ReconciliationResultItem::reference, ReconciliationResultItem::status)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("RECON-MATCHED", ReconciliationStatus.MATCHED),
                        org.assertj.core.groups.Tuple.tuple("RECON-MISMATCH", ReconciliationStatus.AMOUNT_MISMATCH),
                        org.assertj.core.groups.Tuple.tuple("RECON-MISSING-SETTLEMENT", ReconciliationStatus.MISSING_IN_SETTLEMENT),
                        org.assertj.core.groups.Tuple.tuple("RECON-ORPHAN", ReconciliationStatus.MISSING_IN_LEDGER));
    }

    private void initiate(Account source, String reference, String destinationAccount, BigDecimal amount) throws Exception {
        InitiateTransferRequest request = new InitiateTransferRequest(reference, source.getId(), "000001",
                destinationAccount, amount, "reconciliation test");
        mockMvc.perform(post("/api/nip/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
