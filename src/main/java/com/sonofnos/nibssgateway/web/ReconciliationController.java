package com.sonofnos.nibssgateway.web;

import com.sonofnos.nibssgateway.reconciliation.ReconciliationResultItem;
import com.sonofnos.nibssgateway.reconciliation.ReconciliationService;
import com.sonofnos.nibssgateway.reconciliation.SettlementCsvParser;
import com.sonofnos.nibssgateway.reconciliation.SettlementRecord;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final SettlementCsvParser settlementCsvParser;
    private final ReconciliationService reconciliationService;

    public ReconciliationController(SettlementCsvParser settlementCsvParser, ReconciliationService reconciliationService) {
        this.settlementCsvParser = settlementCsvParser;
        this.reconciliationService = reconciliationService;
    }

    @PostMapping(value = "/runs", consumes = "multipart/form-data")
    public List<ReconciliationResultItem> run(@RequestParam("file") MultipartFile file) throws IOException {
        List<SettlementRecord> records = settlementCsvParser.parse(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        return reconciliationService.reconcile(records);
    }
}
