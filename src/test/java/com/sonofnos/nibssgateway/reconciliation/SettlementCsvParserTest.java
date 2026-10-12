package com.sonofnos.nibssgateway.reconciliation;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementCsvParserTest {

    private final SettlementCsvParser parser = new SettlementCsvParser();

    @Test
    void parsesRowsAfterHeader() {
        String csv = "reference,amount\nREF-1,100.00\nREF-2,250.50\n";
        List<SettlementRecord> records = parser.parse(new StringReader(csv));

        assertThat(records).containsExactly(
                new SettlementRecord("REF-1", new BigDecimal("100.00")),
                new SettlementRecord("REF-2", new BigDecimal("250.50")));
    }

    @Test
    void parsesWithoutHeader() {
        String csv = "REF-1,100.00\n";
        List<SettlementRecord> records = parser.parse(new StringReader(csv));

        assertThat(records).containsExactly(new SettlementRecord("REF-1", new BigDecimal("100.00")));
    }

    @Test
    void skipsBlankLines() {
        String csv = "reference,amount\n\nREF-1,100.00\n\n";
        List<SettlementRecord> records = parser.parse(new StringReader(csv));

        assertThat(records).hasSize(1);
    }
}
