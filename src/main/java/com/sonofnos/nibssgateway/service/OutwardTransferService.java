package com.sonofnos.nibssgateway.service;

import com.sonofnos.nibssgateway.aml.AmlScreeningProvider;
import com.sonofnos.nibssgateway.aml.ScreeningResult;
import com.sonofnos.nibssgateway.audit.AuditLogService;
import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.domain.TransferStatus;
import com.sonofnos.nibssgateway.ledger.LedgerService;
import com.sonofnos.nibssgateway.ledger.SettlementAccounts;
import com.sonofnos.nibssgateway.nip.*;
import com.sonofnos.nibssgateway.repository.TransferRepository;
import com.sonofnos.nibssgateway.web.dto.InitiateTransferRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Orchestrates an outward NIP transfer: idempotent accept, AML
 * screening, debit + NIBSS funds transfer, and the requery-not-retry
 * handling of a NIBSS timeout.
 *
 * <p>Each database-touching step runs in its own short transaction via
 * TransactionTemplate rather than one long @Transactional method,
 * specifically so the NIBSS call (sendFundsTransfer, queryTransactionStatus)
 * never happens while holding open a database transaction - a slow or
 * timed-out "NIBSS" call must not also hold row locks or a connection
 * from the pool.
 */
@Service
public class OutwardTransferService {

    private final TransferRepository transferRepository;
    private final LedgerService ledgerService;
    private final NibssNipClient nibssNipClient;
    private final AmlScreeningProvider amlScreeningProvider;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;

    public OutwardTransferService(TransferRepository transferRepository, LedgerService ledgerService,
                                   NibssNipClient nibssNipClient, AmlScreeningProvider amlScreeningProvider,
                                   AuditLogService auditLogService, PlatformTransactionManager transactionManager) {
        this.transferRepository = transferRepository;
        this.ledgerService = ledgerService;
        this.nibssNipClient = nibssNipClient;
        this.amlScreeningProvider = amlScreeningProvider;
        this.auditLogService = auditLogService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public Transfer initiate(InitiateTransferRequest request) {
        Transfer newTransfer = new Transfer(request.clientReference(), request.sourceAccountId(),
                request.destinationBankCode(), request.destinationAccountNumber(), null,
                request.amount(), request.narration());

        Transfer transfer;
        try {
            transfer = transactionTemplate.execute(status -> transferRepository.save(newTransfer));
        } catch (DataIntegrityViolationException alreadyExists) {
            // Same client reference replayed: the unique constraint on
            // client_reference is what caught it, not a prior lookup.
            return transferRepository.findByClientReference(request.clientReference())
                    .orElseThrow(() -> alreadyExists);
        }
        UUID transferId = transfer.getId();
        auditLogService.record("TRANSFER_INITIATED", "Transfer", transferId.toString(),
                "amount=" + request.amount() + " destinationBank=" + request.destinationBankCode());

        ScreeningResult screening = amlScreeningProvider.screen(request.destinationAccountNumber(), request.amount());
        if (screening.isFlagged()) {
            updateStatus(transferId, TransferStatus.PENDING_REVIEW, null);
            auditLogService.record("TRANSFER_SCREENED", "Transfer", transferId.toString(),
                    "FLAGGED: " + screening.reason());
            return requireTransfer(transferId);
        }
        auditLogService.record("TRANSFER_SCREENED", "Transfer", transferId.toString(), "PASS");

        NameEnquiryResult nameEnquiry = nibssNipClient.nameEnquiry(request.destinationBankCode(),
                request.destinationAccountNumber());
        String sessionId = nameEnquiry.sessionId();

        transactionTemplate.executeWithoutResult(status -> {
            Transfer t = transferRepository.findById(transferId).orElseThrow();
            ledgerService.post(t.getSourceAccountId(), SettlementAccounts.NIBSS_SETTLEMENT_SUSPENSE, t.getAmount(),
                    t.getClientReference(), "Outward NIP transfer to " + t.getDestinationBankCode());
            t.assignSessionId(sessionId);
            t.moveTo(TransferStatus.PROCESSING);
            transferRepository.save(t);
        });
        auditLogService.record("TRANSFER_DEBITED", "Transfer", transferId.toString(), "sessionId=" + sessionId);

        try {
            TransferOutcome outcome = nibssNipClient.sendFundsTransfer(sessionId, request.destinationBankCode(),
                    request.destinationAccountNumber(), request.amount());
            applyOutcome(transferId, outcome.code());
        } catch (NibssTimeoutException timeout) {
            // The documented NIBSS failure mode: on a timeout, requery
            // (TSQ) the transaction status. Never resend the transfer -
            // that is exactly what causes a double-send on interbank
            // rails if the first transfer actually landed.
            requeryAfterTimeout(transferId, sessionId);
        }
        return requireTransfer(transferId);
    }

    private void requeryAfterTimeout(UUID transferId, String sessionId) {
        transactionTemplate.executeWithoutResult(status -> {
            Transfer t = transferRepository.findById(transferId).orElseThrow();
            t.moveTo(TransferStatus.REQUERY_PENDING);
            t.incrementRequery();
            transferRepository.save(t);
        });
        auditLogService.record("TRANSFER_REQUERY", "Transfer", transferId.toString(),
                "NIBSS timed out; querying TSQ instead of resending the transfer");

        TsqResult tsq = nibssNipClient.queryTransactionStatus(sessionId);
        applyOutcome(transferId, tsq.code());
    }

    private void applyOutcome(UUID transferId, NipResponseCode code) {
        transactionTemplate.executeWithoutResult(status -> {
            Transfer t = transferRepository.findById(transferId).orElseThrow();
            switch (code) {
                case APPROVED -> t.moveTo(TransferStatus.SUCCESSFUL);
                case DEFINITE_FAILURE -> {
                    // Correcting double-entry: credit the source back,
                    // debit the suspense account. The original debit
                    // posting is never deleted.
                    ledgerService.post(SettlementAccounts.NIBSS_SETTLEMENT_SUSPENSE, t.getSourceAccountId(),
                            t.getAmount(), t.getClientReference() + ":failed-reversal",
                            "Reversal of failed outward transfer");
                    t.moveTo(TransferStatus.FAILED);
                }
                case UNKNOWN -> t.moveTo(TransferStatus.REQUERY_PENDING);
            }
            transferRepository.save(t);
        });
        auditLogService.record("TRANSFER_OUTCOME", "Transfer", transferId.toString(), code.toString());
    }

    private void updateStatus(UUID transferId, TransferStatus status, String sessionId) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            Transfer t = transferRepository.findById(transferId).orElseThrow();
            t.moveTo(status);
            if (sessionId != null) {
                t.assignSessionId(sessionId);
            }
            transferRepository.save(t);
        });
    }

    private Transfer requireTransfer(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new NoSuchElementException("No transfer " + transferId));
    }
}
