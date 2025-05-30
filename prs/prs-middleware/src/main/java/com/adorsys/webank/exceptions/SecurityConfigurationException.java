package com.adorsys.webank.exceptions;


public class SecurityConfigurationException extends RuntimeException {
    public SecurityConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}