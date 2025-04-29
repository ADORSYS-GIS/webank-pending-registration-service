package com.adorsys.webank.exceptions;
public class KycProcessingException extends RuntimeException {
    public KycProcessingException(String message) {
        super(message);
    }

    public KycProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}