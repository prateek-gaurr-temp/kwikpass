package com.gk.kwikpass.smsuserconsent;

public class SmsUserConsentException extends Exception {
    private final Errors code;

    public SmsUserConsentException(Errors code, String message) {
        super(message);
        this.code = code;
    }

    public Errors getCode() {
        return code;
    }
} 