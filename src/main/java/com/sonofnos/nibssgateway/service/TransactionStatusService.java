package com.sonofnos.nibssgateway.service;

import com.sonofnos.nibssgateway.audit.AuditLogService;
import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.nip.NibssNipClient;
import com.sonofnos.nibssgateway.nip.TsqResult;
import com.sonofnos.nibssgateway.repository.TransferRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Standalone TSQ: query NIBSS for the real status of a previously
 * initiated transfer by client reference. Used both as a manual
 * reconciliation tool and internally by OutwardTransferService's
 * requery-on-timeout path.
 */
@Service
public class TransactionStatusService {

    private final TransferRepository transferRepository;
    private final NibssNipClient nibssNipClient;
    private final AuditLogService auditLogService;

    public TransactionStatusService(TransferRepository transferRepository, NibssNipClient nibssNipClient,
                                     AuditLogService auditLogService) {
        this.transferRepository = transferRepository;
        this.nibssNipClient = nibssNipClient;
        this.auditLogService = auditLogService;
    }

    public TsqResult query(String clientReference) {
        Transfer transfer = transferRepository.findByClientReference(clientReference)
                .orElseThrow(() -> new NoSuchElementException("No transfer with reference " + clientReference));
        if (transfer.getNibssSessionId() == null) {
            throw new IllegalStateException("Transfer " + clientReference + " was never sent to NIBSS yet");
        }
        TsqResult result = nibssNipClient.queryTransactionStatus(transfer.getNibssSessionId());
        auditLogService.record("TSQ_QUERIED", "Transfer", transfer.getId().toString(), result.code().toString());
        return result;
    }
}
