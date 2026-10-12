package com.sonofnos.nibssgateway.web;

import com.sonofnos.nibssgateway.nip.NameEnquiryResult;
import com.sonofnos.nibssgateway.service.NameEnquiryService;
import com.sonofnos.nibssgateway.web.dto.NameEnquiryRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nip/name-enquiry")
public class NameEnquiryController {

    private final NameEnquiryService nameEnquiryService;

    public NameEnquiryController(NameEnquiryService nameEnquiryService) {
        this.nameEnquiryService = nameEnquiryService;
    }

    @PostMapping
    public NameEnquiryResult nameEnquiry(@Valid @RequestBody NameEnquiryRequest request) {
        return nameEnquiryService.enquire(request.destinationBankCode(), request.accountNumber());
    }
}
