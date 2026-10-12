package com.sonofnos.nibssgateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonofnos.nibssgateway.domain.InwardCredit;
import com.sonofnos.nibssgateway.domain.Reversal;
import com.sonofnos.nibssgateway.security.HmacSignatureService;
import com.sonofnos.nibssgateway.service.InwardCreditService;
import com.sonofnos.nibssgateway.service.ReversalService;
import com.sonofnos.nibssgateway.web.dto.InwardCreditWebhookRequest;
import com.sonofnos.nibssgateway.web.dto.ReversalWebhookRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * NIBSS-style inbound webhooks. Both are HMAC-SHA256 signed; the raw
 * request body is read as a string and verified before being parsed
 * into a DTO, so the bytes being verified are exactly the bytes that
 * were signed.
 */
@RestController
@RequestMapping("/api/nip/webhooks")
public class WebhookController {

    private final HmacSignatureService hmacSignatureService;
    private final InwardCreditService inwardCreditService;
    private final ReversalService reversalService;
    private final ObjectMapper objectMapper;

    public WebhookController(HmacSignatureService hmacSignatureService, InwardCreditService inwardCreditService,
                              ReversalService reversalService, ObjectMapper objectMapper) {
        this.hmacSignatureService = hmacSignatureService;
        this.inwardCreditService = inwardCreditService;
        this.reversalService = reversalService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/inward-credits")
    public ResponseEntity<InwardCredit> inwardCredit(@RequestBody String rawBody,
                                                       @RequestHeader("X-NIBSS-Signature") String signature) throws Exception {
        if (!hmacSignatureService.isValid(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        InwardCreditWebhookRequest request = objectMapper.readValue(rawBody, InwardCreditWebhookRequest.class);
        return ResponseEntity.ok(inwardCreditService.process(request));
    }

    @PostMapping("/reversals")
    public ResponseEntity<Reversal> reversal(@RequestBody String rawBody,
                                              @RequestHeader("X-NIBSS-Signature") String signature) throws Exception {
        if (!hmacSignatureService.isValid(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ReversalWebhookRequest request = objectMapper.readValue(rawBody, ReversalWebhookRequest.class);
        return ResponseEntity.ok(reversalService.process(request));
    }
}
