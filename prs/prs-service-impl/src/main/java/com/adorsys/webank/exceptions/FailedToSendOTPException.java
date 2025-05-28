package com.adorsys.webank.exceptions;

public class FailedToSendOTPException extends RuntimeException {
    public FailedToSendOTPException(String message) {
        super(message);
    }
    
    public FailedToSendOTPException(String message, Throwable cause) {
        super(message, cause);
    }
}
