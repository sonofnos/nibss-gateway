package com.sonofnos.nibssgateway.nip;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic in-process stand-in for NIBSS NIP. This is simulated:
 * there is no real NIBSS connection anywhere in this repo, no sponsor
 * bank, no live institution code. The "mock bank registry" below is a
 * hardcoded map of a few fake banks and accounts, enough to exercise
 * name enquiry, outward transfer, TSQ, inward credit and reversal end
 * to end.
 *
 * <p>Two account numbers are magic, used by tests to drive specific
 * behaviour deterministically:
 * <ul>
 *   <li>{@value #TIMEOUT_THEN_SUCCESS_ACCOUNT} - sendFundsTransfer throws
 *   NibssTimeoutException the first time; queryTransactionStatus for
 *   that session then reports APPROVED, simulating a transfer that
 *   actually landed on NIBSS's side despite the caller never getting a
 *   response.</li>
 *   <li>{@value #TIMEOUT_THEN_FAILURE_ACCOUNT} - same timeout, but TSQ
 *   later reports DEFINITE_FAILURE, simulating a transfer that never
 *   landed.</li>
 * </ul>
 */
@Component
public class FakeNibssNipClient implements NibssNipClient {

    public static final String TIMEOUT_THEN_SUCCESS_ACCOUNT = "4000000001";
    public static final String TIMEOUT_THEN_FAILURE_ACCOUNT = "4000000002";

    private static final Map<String, Map<String, String>> BANK_REGISTRY = Map.of(
            "000001", Map.of(
                    "0123456789", "ADAEZE OKONKWO",
                    "1122334455", "CHUKWUDI EZE",
                    TIMEOUT_THEN_SUCCESS_ACCOUNT, "TIMEOUT TEST ACCOUNT (SUCCEEDS ON REQUERY)",
                    TIMEOUT_THEN_FAILURE_ACCOUNT, "TIMEOUT TEST ACCOUNT (FAILS ON REQUERY)"
            ),
            "000002", Map.of(
                    "9988776655", "BLESSING UDOH"
            )
    );

    /** sessionId -> the outcome TSQ should report once requeried. */
    private final Map<String, TsqResult> pendingOutcomes = new ConcurrentHashMap<>();

    @Override
    public NameEnquiryResult nameEnquiry(String destinationBankCode, String accountNumber) {
        Map<String, String> bank = BANK_REGISTRY.get(destinationBankCode);
        String name = bank == null ? null : bank.get(accountNumber);
        if (name == null) {
            throw new AccountNotFoundException(destinationBankCode, accountNumber);
        }
        return new NameEnquiryResult(destinationBankCode, accountNumber, name, newSessionId(destinationBankCode));
    }

    @Override
    public TransferOutcome sendFundsTransfer(String sessionId, String destinationBankCode,
                                              String destinationAccountNumber, BigDecimal amount) {
        if (TIMEOUT_THEN_SUCCESS_ACCOUNT.equals(destinationAccountNumber)) {
            pendingOutcomes.put(sessionId, new TsqResult(NipResponseCode.APPROVED, sessionId,
                    "00 - Approved (resolved by requery, not by resend)"));
            throw new NibssTimeoutException(sessionId);
        }
        if (TIMEOUT_THEN_FAILURE_ACCOUNT.equals(destinationAccountNumber)) {
            pendingOutcomes.put(sessionId, new TsqResult(NipResponseCode.DEFINITE_FAILURE, sessionId,
                    "51 - No sufficient funds at destination (resolved by requery)"));
            throw new NibssTimeoutException(sessionId);
        }
        return new TransferOutcome(NipResponseCode.APPROVED, sessionId, "00 - Approved");
    }

    @Override
    public TsqResult queryTransactionStatus(String sessionId) {
        TsqResult pending = pendingOutcomes.get(sessionId);
        if (pending != null) {
            return pending;
        }
        return new TsqResult(NipResponseCode.APPROVED, sessionId, "00 - Approved");
    }

    private String newSessionId(String institutionCode) {
        String timestamp = ZonedDateTime.now(ZoneId.of("Africa/Lagos"))
                .format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String unique = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        if (unique.length() < 12) {
            unique = unique + "000000000000";
        }
        return institutionCode + timestamp + unique.substring(0, 12);
    }
}
