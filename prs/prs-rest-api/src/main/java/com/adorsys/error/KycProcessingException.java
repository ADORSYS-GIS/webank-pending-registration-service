package com.adorsys.error;

public class KycProcessingException extends BaseException {
    public KycProcessingException(String message) {
        super(ErrorCode.KYC_PROCESSING_ERROR, message);
    }

    public KycProcessingException(String message, Throwable cause) {
        super(ErrorCode.KYC_PROCESSING_ERROR, message + ": " + cause.getMessage());
    }
} 