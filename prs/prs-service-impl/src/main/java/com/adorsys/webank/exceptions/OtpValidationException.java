/*
 * Copyright (c) 2018-2023 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package com.adorsys.webank.exceptions;

/**
 * Exception thrown when OTP validation fails for any reason.
 */
public class OtpValidationException extends RuntimeException {

    public OtpValidationException(String message) {
        super(message);
    }

    public OtpValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}