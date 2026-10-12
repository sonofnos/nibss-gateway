package com.sonofnos.nibssgateway.reconciliation;

import com.sonofnos.nibssgateway.audit.AuditLogService;
import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AuditLogService auditLogService;

    @Test
    void classifiesAllFourCategories() {
        Transfer matched = transfer("REF-MATCHED", new BigDecimal("100.00"));
        Transfer mismatch = transfer("REF-MISMATCH", new BigDecimal("200.00"));
        when(mismatch.getId()).thenReturn(UUID.randomUUID());
        Transfer missingInSettlement = transfer("REF-MISSING-SETTLEMENT", new BigDecimal("300.00"));

        when(transferRepository.findAllByOrderByCreatedAtAsc())
                .thenReturn(List.of(matched, mismatch, missingInSettlement));

        List<SettlementRecord> settlementFile = List.of(
                new SettlementRecord("REF-MATCHED", new BigDecimal("100.00")),
                new SettlementRecord("REF-MISMATCH", new BigDecimal("199.00")),
                new SettlementRecord("REF-ORPHAN", new BigDecimal("50.00")));

        ReconciliationService service = new ReconciliationService(transferRepository, auditLogService);
        List<ReconciliationResultItem> results = service.reconcile(settlementFile);

        assertThat(results).extracting(ReconciliationResultItem::reference, ReconciliationResultItem::status)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("REF-MATCHED", ReconciliationStatus.MATCHED),
                        org.assertj.core.groups.Tuple.tuple("REF-MISMATCH", ReconciliationStatus.AMOUNT_MISMATCH),
                        org.assertj.core.groups.Tuple.tuple("REF-MISSING-SETTLEMENT", ReconciliationStatus.MISSING_IN_SETTLEMENT),
                        org.assertj.core.groups.Tuple.tuple("REF-ORPHAN", ReconciliationStatus.MISSING_IN_LEDGER));

        verify(auditLogService).record(eq("RECONCILIATION_MISMATCH"), eq("Transfer"), any(), anyString());
        verify(auditLogService).record(eq("RECONCILIATION_ORPHAN"), eq("SettlementRecord"), eq("REF-ORPHAN"), anyString());
    }

    private Transfer transfer(String reference, BigDecimal amount) {
        Transfer transfer = mock(Transfer.class);
        when(transfer.getClientReference()).thenReturn(reference);
        when(transfer.getAmount()).thenReturn(amount);
        return transfer;
    }
}
