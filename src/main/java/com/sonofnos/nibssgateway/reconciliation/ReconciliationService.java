package com.sonofnos.nibssgateway.reconciliation;

import com.sonofnos.nibssgateway.audit.AuditLogService;
import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.repository.TransferRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classifies this gateway's own transfer records against a daily NIBSS
 * settlement file. Matching is by client reference.
 *
 * <p>Exception handling per category:
 * <ul>
 *   <li>MATCHED - no action.</li>
 *   <li>AMOUNT_MISMATCH - audit-logged as RECONCILIATION_MISMATCH for
 *   manual review. The transfer's own status is left alone; this is a
 *   reporting discrepancy until a human confirms which side is wrong,
 *   not a reason to silently change the ledger.</li>
 *   <li>MISSING_IN_LEDGER - audit-logged as RECONCILIATION_ORPHAN. The
 *   settlement file claims a transaction this gateway never recorded,
 *   which in a real deployment would be escalated as a possible fraud
 *   or integration gap, not written into the ledger automatically.</li>
 *   <li>MISSING_IN_SETTLEMENT - left pending, no audit alert by itself.
 *   NIBSS settlement files run on a cycle, so a transfer settling on a
 *   later day is normal; this is exactly the set of references a
 *   standalone TSQ (TransactionStatusService) should be run against if
 *   it stays missing for more than one cycle.</li>
 * </ul>
 */
@Service
public class ReconciliationService {

    private final TransferRepository transferRepository;
    private final AuditLogService auditLogService;

    public ReconciliationService(TransferRepository transferRepository, AuditLogService auditLogService) {
        this.transferRepository = transferRepository;
        this.auditLogService = auditLogService;
    }

    public List<ReconciliationResultItem> reconcile(List<SettlementRecord> settlementRecords) {
        Map<String, SettlementRecord> settlementByReference = new HashMap<>();
        for (SettlementRecord record : settlementRecords) {
            settlementByReference.put(record.reference(), record);
        }

        List<ReconciliationResultItem> results = new ArrayList<>();
        List<Transfer> transfers = transferRepository.findAllByOrderByCreatedAtAsc();

        for (Transfer transfer : transfers) {
            SettlementRecord settlement = settlementByReference.remove(transfer.getClientReference());
            if (settlement == null) {
                results.add(new ReconciliationResultItem(transfer.getClientReference(),
                        ReconciliationStatus.MISSING_IN_SETTLEMENT, transfer.getAmount(), null,
                        "Not present in this settlement file; may settle on a later cycle"));
                continue;
            }
            if (transfer.getAmount().compareTo(settlement.amount()) == 0) {
                results.add(new ReconciliationResultItem(transfer.getClientReference(),
                        ReconciliationStatus.MATCHED, transfer.getAmount(), settlement.amount(), null));
            } else {
                results.add(new ReconciliationResultItem(transfer.getClientReference(),
                        ReconciliationStatus.AMOUNT_MISMATCH, transfer.getAmount(), settlement.amount(),
                        "Ledger and settlement amounts disagree"));
                auditLogService.record("RECONCILIATION_MISMATCH", "Transfer", transfer.getId().toString(),
                        "ledger=" + transfer.getAmount() + " settlement=" + settlement.amount());
            }
        }

        for (SettlementRecord orphan : settlementByReference.values()) {
            results.add(new ReconciliationResultItem(orphan.reference(), ReconciliationStatus.MISSING_IN_LEDGER,
                    null, orphan.amount(), "Settlement file has this reference; no transfer recorded in the ledger"));
            auditLogService.record("RECONCILIATION_ORPHAN", "SettlementRecord", orphan.reference(),
                    "settlementAmount=" + orphan.amount());
        }

        return results;
    }
}
