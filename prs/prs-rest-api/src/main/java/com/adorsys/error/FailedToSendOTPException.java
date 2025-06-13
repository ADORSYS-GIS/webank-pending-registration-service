package com.adorsys.error;

public class FailedToSendOTPException extends BaseException {
    public FailedToSendOTPException(String message) {
        super(ErrorCode.OTP_SEND_ERROR, message);
    }
} 