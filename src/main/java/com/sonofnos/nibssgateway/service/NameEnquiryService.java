package com.sonofnos.nibssgateway.service;

import com.sonofnos.nibssgateway.audit.AuditLogService;
import com.sonofnos.nibssgateway.nip.NameEnquiryResult;
import com.sonofnos.nibssgateway.nip.NibssNipClient;
import org.springframework.stereotype.Service;

@Service
public class NameEnquiryService {

    private final NibssNipClient nibssNipClient;
    private final AuditLogService auditLogService;

    public NameEnquiryService(NibssNipClient nibssNipClient, AuditLogService auditLogService) {
        this.nibssNipClient = nibssNipClient;
        this.auditLogService = auditLogService;
    }

    public NameEnquiryResult enquire(String destinationBankCode, String accountNumber) {
        NameEnquiryResult result = nibssNipClient.nameEnquiry(destinationBankCode, accountNumber);
        auditLogService.record("NAME_ENQUIRY", "Account", destinationBankCode + ":" + accountNumber,
                "resolved=" + result.accountName());
        return result;
    }
}
