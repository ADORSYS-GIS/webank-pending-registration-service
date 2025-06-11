/*
 * Copyright (c) 2018-2023 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package com.adorsys.webank.exceptions;

/**
 * Exception thrown when there is an error during OTP processing.
 */
public class OtpProcessingException extends RuntimeException {

    public OtpProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

}
