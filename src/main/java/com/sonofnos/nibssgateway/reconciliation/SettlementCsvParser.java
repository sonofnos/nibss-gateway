package com.sonofnos.nibssgateway.reconciliation;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the daily settlement file: a header row (reference,amount)
 * followed by one row per settled transaction. Same shape as the CSV
 * reconciliation logic in reconcile-desk/naira-rails.
 */
@Component
public class SettlementCsvParser {

    public List<SettlementRecord> parse(Reader reader) {
        List<SettlementRecord> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (first) {
                    first = false;
                    if (line.toLowerCase().startsWith("reference")) {
                        continue;
                    }
                }
                String[] parts = line.split(",", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Malformed settlement row: " + line);
                }
                records.add(new SettlementRecord(parts[0].trim(), new BigDecimal(parts[1].trim())));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read settlement file", e);
        }
        return records;
    }
}
