package com.adorsys.webank.exceptions;

public class FailedToSendOTPException extends RuntimeException {
    public FailedToSendOTPException(String message) {
        super(message);
    }
}
