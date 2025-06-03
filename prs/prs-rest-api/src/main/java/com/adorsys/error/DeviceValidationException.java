package com.adorsys.error;

public class DeviceValidationException extends BaseException {
    public DeviceValidationException(String message) {
        super(ErrorCode.DEVICE_VALIDATION_FAILED, message);
    }
} 