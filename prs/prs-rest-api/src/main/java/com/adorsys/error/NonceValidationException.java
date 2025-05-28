package com.adorsys.error;

public class NonceValidationException extends BaseException {
    public NonceValidationException(String message) {
        super(ErrorCode.INVALID_NONCE, message);
    }
} 