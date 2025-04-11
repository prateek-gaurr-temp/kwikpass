package com.gk.kwikpass.smsuserconsent;

public class SmsUserConsentException extends Exception {
    private final SmsUserConsentManager.ErrorCode errorCode;

    public SmsUserConsentException(SmsUserConsentManager.ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SmsUserConsentManager.ErrorCode getErrorCode() {
        return errorCode;
    }
} 