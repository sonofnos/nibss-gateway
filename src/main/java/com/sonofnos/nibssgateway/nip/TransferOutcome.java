package com.sonofnos.nibssgateway.nip;

public record TransferOutcome(NipResponseCode code, String sessionId, String narrative) {
}
