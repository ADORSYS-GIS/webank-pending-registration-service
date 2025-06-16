package com.adorsys.error;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
} 