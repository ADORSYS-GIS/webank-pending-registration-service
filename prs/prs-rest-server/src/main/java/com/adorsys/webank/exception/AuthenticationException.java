package com.adorsys.webank.exception;

/**
 * Exception thrown when authentication fails, such as invalid JWT tokens
 * or unauthorized access attempts.
 */
public class AuthenticationException extends SecurityException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
} 