package com.sonofnos.nibssgateway.service;

import com.sonofnos.nibssgateway.aml.AmlScreeningProvider;
import com.sonofnos.nibssgateway.aml.ScreeningDecision;
import com.sonofnos.nibssgateway.aml.ScreeningResult;
import com.sonofnos.nibssgateway.audit.AuditLogService;
import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.domain.TransferStatus;
import com.sonofnos.nibssgateway.ledger.LedgerService;
import com.sonofnos.nibssgateway.nip.*;
import com.sonofnos.nibssgateway.repository.TransferRepository;
import com.sonofnos.nibssgateway.web.dto.InitiateTransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit-level proof of the requery-not-retry rule: on a simulated NIBSS
 * timeout, OutwardTransferService must call queryTransactionStatus
 * (TSQ) and must NOT call sendFundsTransfer a second time for the same
 * transfer.
 */
@ExtendWith(MockitoExtension.class)
class OutwardTransferServiceTest {

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private NibssNipClient nibssNipClient;
    @Mock
    private AmlScreeningProvider amlScreeningProvider;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private OutwardTransferService service;
    private Transfer transfer;

    @BeforeEach
    void setUp() throws Exception {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

        InitiateTransferRequest request = new InitiateTransferRequest("CLIENT-REF-1", UUID.randomUUID(),
                "000001", FakeNibssNipClient.TIMEOUT_THEN_SUCCESS_ACCOUNT, new BigDecimal("500.00"), "test");

        transfer = new Transfer(request.clientReference(), request.sourceAccountId(), request.destinationBankCode(),
                request.destinationAccountNumber(), null, request.amount(), request.narration());
        setId(transfer, UUID.randomUUID());

        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(transferRepository.findById(any())).thenReturn(Optional.of(transfer));

        lenient().when(amlScreeningProvider.screen(anyString(), any(BigDecimal.class)))
                .thenReturn(new ScreeningResult(ScreeningDecision.PASS, null));

        service = new OutwardTransferService(transferRepository, ledgerService, nibssNipClient,
                amlScreeningProvider, auditLogService, transactionManager);

        this.request = request;
    }

    private InitiateTransferRequest request;

    @Test
    void timeoutTriggersRequeryNotResend() {
        when(nibssNipClient.nameEnquiry(anyString(), anyString()))
                .thenReturn(new NameEnquiryResult("000001", request.destinationAccountNumber(), "TIMEOUT TEST ACCOUNT", "SESSION-1"));
        when(nibssNipClient.sendFundsTransfer(anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new NibssTimeoutException("SESSION-1"));
        when(nibssNipClient.queryTransactionStatus("SESSION-1"))
                .thenReturn(new TsqResult(NipResponseCode.APPROVED, "SESSION-1", "00 - Approved on requery"));

        Transfer result = service.initiate(request);

        verify(nibssNipClient, times(1)).sendFundsTransfer(anyString(), anyString(), anyString(), any(BigDecimal.class));
        verify(nibssNipClient, times(1)).queryTransactionStatus("SESSION-1");
        assertThat(result.getStatus()).isEqualTo(TransferStatus.SUCCESSFUL);
    }

    @Test
    void definiteFailureOnRequeryReversesTheDebit() {
        when(nibssNipClient.nameEnquiry(anyString(), anyString()))
                .thenReturn(new NameEnquiryResult("000001", request.destinationAccountNumber(), "TIMEOUT TEST ACCOUNT", "SESSION-1"));
        when(nibssNipClient.sendFundsTransfer(anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new NibssTimeoutException("SESSION-1"));
        when(nibssNipClient.queryTransactionStatus("SESSION-1"))
                .thenReturn(new TsqResult(NipResponseCode.DEFINITE_FAILURE, "SESSION-1", "51 - failed"));

        Transfer result = service.initiate(request);

        verify(nibssNipClient, times(1)).sendFundsTransfer(anyString(), anyString(), anyString(), any(BigDecimal.class));
        // Once for the original debit, once for the correcting reversal entry.
        verify(ledgerService, times(2)).post(any(), any(), eq(request.amount()), anyString(), anyString());
        assertThat(result.getStatus()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void flaggedTransferGoesToPendingReviewWithoutTouchingNibss() {
        when(amlScreeningProvider.screen(anyString(), any(BigDecimal.class)))
                .thenReturn(new ScreeningResult(ScreeningDecision.FLAGGED, "over threshold"));

        Transfer result = service.initiate(request);

        verifyNoInteractions(nibssNipClient);
        verify(ledgerService, never()).post(any(), any(), any(), anyString(), anyString());
        assertThat(result.getStatus()).isEqualTo(TransferStatus.PENDING_REVIEW);
    }

    private static void setId(Transfer transfer, UUID id) throws Exception {
        Field field = Transfer.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(transfer, id);
    }
}
