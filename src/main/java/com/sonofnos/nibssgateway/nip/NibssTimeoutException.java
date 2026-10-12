package com.sonofnos.nibssgateway.nip;

/**
 * Thrown by NibssNipClient.sendFundsTransfer when the simulated NIBSS
 * endpoint does not answer in time. A timeout is not a failure and not
 * a success - the money may already have moved on NIBSS's side even
 * though this call never got a response. The only correct next step is
 * TSQ, never resubmitting the transfer.
 */
public class NibssTimeoutException extends RuntimeException {

    private final String sessionId;

    public NibssTimeoutException(String sessionId) {
        super("NIBSS NIP funds transfer timed out for session " + sessionId);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
