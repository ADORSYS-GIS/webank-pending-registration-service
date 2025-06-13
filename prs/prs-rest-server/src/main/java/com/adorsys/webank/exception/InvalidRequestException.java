package com.adorsys.webank.exception;

/**
 * Exception thrown when a request contains invalid or missing data.
 */
public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
} 