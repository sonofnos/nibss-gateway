package com.sonofnos.nibssgateway.service;

import com.sonofnos.nibssgateway.audit.AuditLogService;
import com.sonofnos.nibssgateway.domain.Reversal;
import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.domain.TransferStatus;
import com.sonofnos.nibssgateway.ledger.LedgerService;
import com.sonofnos.nibssgateway.ledger.SettlementAccounts;
import com.sonofnos.nibssgateway.repository.ReversalRepository;
import com.sonofnos.nibssgateway.repository.TransferRepository;
import com.sonofnos.nibssgateway.web.dto.ReversalWebhookRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.NoSuchElementException;

/**
 * Handles a NIBSS-initiated reversal of a previously successful
 * outward transfer (destination account does not exist, or a failed
 * inward credit on the other side needs the original debit reversed).
 * Idempotent on nibssReference, same unique-constraint pattern as
 * InwardCreditService. The reversal posts a correcting double-entry -
 * it never deletes the original transfer's ledger rows.
 */
@Service
public class ReversalService {

    private final ReversalRepository reversalRepository;
    private final TransferRepository transferRepository;
    private final LedgerService ledgerService;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;

    public ReversalService(ReversalRepository reversalRepository, TransferRepository transferRepository,
                            LedgerService ledgerService, AuditLogService auditLogService,
                            PlatformTransactionManager transactionManager) {
        this.reversalRepository = reversalRepository;
        this.transferRepository = transferRepository;
        this.ledgerService = ledgerService;
        this.auditLogService = auditLogService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public Reversal process(ReversalWebhookRequest request) {
        try {
            Reversal reversal = transactionTemplate.execute(status -> {
                Transfer transfer = transferRepository.findByNibssSessionId(request.nibssSessionId())
                        .orElseThrow(() -> new NoSuchElementException(
                                "No transfer for NIBSS session " + request.nibssSessionId()));

                Reversal saved = reversalRepository.save(new Reversal(request.nibssReference(), transfer.getId(),
                        request.reason(), transfer.getAmount()));

                ledgerService.post(SettlementAccounts.NIBSS_SETTLEMENT_SUSPENSE, transfer.getSourceAccountId(),
                        transfer.getAmount(), request.nibssReference(), "Reversal: " + request.reason());

                transfer.moveTo(TransferStatus.REVERSED);
                transferRepository.save(transfer);
                return saved;
            });
            auditLogService.record("TRANSFER_REVERSED", "Reversal", reversal.getId().toString(),
                    "reason=" + request.reason());
            return reversal;
        } catch (DataIntegrityViolationException duplicate) {
            return reversalRepository.findByNibssReference(request.nibssReference())
                    .orElseThrow(() -> duplicate);
        }
    }
}
