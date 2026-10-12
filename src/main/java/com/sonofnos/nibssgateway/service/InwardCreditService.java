package com.sonofnos.nibssgateway.service;

import com.sonofnos.nibssgateway.audit.AuditLogService;
import com.sonofnos.nibssgateway.domain.InwardCredit;
import com.sonofnos.nibssgateway.ledger.LedgerService;
import com.sonofnos.nibssgateway.ledger.SettlementAccounts;
import com.sonofnos.nibssgateway.repository.InwardCreditRepository;
import com.sonofnos.nibssgateway.web.dto.InwardCreditWebhookRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Handles the inward credit webhook: a NIBSS notification of an
 * incoming interbank transfer. Idempotent on nibssReference via the
 * unique DB constraint on inward_credits.nibss_reference - the insert
 * and the ledger posting happen in the same transaction, so a replayed
 * webhook either inserts nothing and posts nothing (constraint violation
 * rolls both back) or does both exactly once. No prior SELECT is used to
 * decide whether to proceed.
 */
@Service
public class InwardCreditService {

    private final InwardCreditRepository inwardCreditRepository;
    private final LedgerService ledgerService;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;

    public InwardCreditService(InwardCreditRepository inwardCreditRepository, LedgerService ledgerService,
                                AuditLogService auditLogService, PlatformTransactionManager transactionManager) {
        this.inwardCreditRepository = inwardCreditRepository;
        this.ledgerService = ledgerService;
        this.auditLogService = auditLogService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public InwardCredit process(InwardCreditWebhookRequest request) {
        try {
            InwardCredit credit = transactionTemplate.execute(status -> {
                InwardCredit saved = inwardCreditRepository.save(new InwardCredit(
                        request.nibssReference(), request.destinationAccountId(), request.sourceBankCode(),
                        request.sourceAccountNumber(), request.amount()));
                ledgerService.post(SettlementAccounts.NIBSS_SETTLEMENT_SUSPENSE, request.destinationAccountId(),
                        request.amount(), request.nibssReference(),
                        "Inward NIP credit from " + request.sourceBankCode());
                return saved;
            });
            auditLogService.record("INWARD_CREDIT_APPLIED", "InwardCredit", credit.getId().toString(),
                    "amount=" + request.amount());
            return credit;
        } catch (DataIntegrityViolationException duplicate) {
            // Replay of a NIBSS reference already processed: return the
            // original record, apply no second ledger effect.
            return inwardCreditRepository.findByNibssReference(request.nibssReference())
                    .orElseThrow(() -> duplicate);
        }
    }
}
