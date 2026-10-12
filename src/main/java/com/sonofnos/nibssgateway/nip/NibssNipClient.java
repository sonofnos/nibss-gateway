package com.sonofnos.nibssgateway.nip;

import java.math.BigDecimal;

/**
 * The boundary to NIBSS's real interbank transfer scheme. This
 * interface is never actually reachable from a laptop - NIBSS NIP
 * requires a sponsor bank connection and real institution credentials -
 * so the only implementation in this repo is FakeNibssNipClient, a
 * deterministic in-process simulator. Documented plainly: no real NIBSS
 * integration exists here, same honest-fake pattern as naira-rails'
 * NipSimulatorClient.
 */
public interface NibssNipClient {

    NameEnquiryResult nameEnquiry(String destinationBankCode, String accountNumber);

    /**
     * Sends a funds transfer (FT) for a session already opened by name
     * enquiry. May throw NibssTimeoutException - callers must requery
     * (TSQ) on that, never call this again for the same sessionId.
     */
    TransferOutcome sendFundsTransfer(String sessionId, String destinationBankCode,
                                       String destinationAccountNumber, BigDecimal amount);

    TsqResult queryTransactionStatus(String sessionId);
}
