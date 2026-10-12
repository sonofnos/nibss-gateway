package com.sonofnos.nibssgateway.nip;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String bankCode, String accountNumber) {
        super("No account " + accountNumber + " at bank " + bankCode + " (simulated NIP name enquiry: 'Invalid Account Number')");
    }
}
