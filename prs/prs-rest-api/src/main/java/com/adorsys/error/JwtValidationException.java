package com.adorsys.error;

public class JwtValidationException extends BaseException {
    public JwtValidationException(String message) {
        super(ErrorCode.INVALID_JWT, message);
    }
} 