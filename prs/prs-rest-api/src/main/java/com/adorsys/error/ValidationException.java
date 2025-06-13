package com.adorsys.error;

public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
} 