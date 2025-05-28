package com.adorsys.error;

public class DeviceRegistrationException extends BaseException {
    public DeviceRegistrationException(String message) {
        super(ErrorCode.DEVICE_REGISTRATION_FAILED, message);
    }
} 