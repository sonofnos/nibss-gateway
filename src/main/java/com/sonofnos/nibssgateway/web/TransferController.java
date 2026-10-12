package com.sonofnos.nibssgateway.web;

import com.sonofnos.nibssgateway.domain.Transfer;
import com.sonofnos.nibssgateway.nip.TsqResult;
import com.sonofnos.nibssgateway.service.OutwardTransferService;
import com.sonofnos.nibssgateway.service.TransactionStatusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nip/transfers")
public class TransferController {

    private final OutwardTransferService outwardTransferService;
    private final TransactionStatusService transactionStatusService;

    public TransferController(OutwardTransferService outwardTransferService,
                               TransactionStatusService transactionStatusService) {
        this.outwardTransferService = outwardTransferService;
        this.transactionStatusService = transactionStatusService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transfer initiate(@Valid @RequestBody com.sonofnos.nibssgateway.web.dto.InitiateTransferRequest request) {
        return outwardTransferService.initiate(request);
    }

    @GetMapping("/{clientReference}/tsq")
    public TsqResult tsq(@PathVariable String clientReference) {
        return transactionStatusService.query(clientReference);
    }
}
