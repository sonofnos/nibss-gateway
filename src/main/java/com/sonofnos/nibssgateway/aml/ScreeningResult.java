package com.sonofnos.nibssgateway.aml;

public record ScreeningResult(ScreeningDecision decision, String reason) {

    public boolean isFlagged() {
        return decision == ScreeningDecision.FLAGGED;
    }
}
