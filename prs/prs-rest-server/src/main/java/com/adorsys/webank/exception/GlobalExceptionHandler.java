/*
 * Copyright (c) 2018-2023 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package com.adorsys.webank.exception;

import com.adorsys.webank.exceptions.OtpValidationException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.exceptions.JwtValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler for the Webank Pending Registration Service.
 * Provides centralized exception handling across all @RequestMapping methods.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles OtpValidationException and returns appropriate HTTP response.
     *
     * @param ex The OtpValidationException that was thrown
     * @return ResponseEntity with error details and BAD_REQUEST status
     */
    @ExceptionHandler(OtpValidationException.class)
    public ResponseEntity<ErrorResponse> handleOtpValidationException(OtpValidationException ex) {
        log.warn("OTP validation failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "OTP_VALIDATION_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles HashComputationException and returns appropriate HTTP response.
     *
     * @param ex The HashComputationException that was thrown
     * @return ResponseEntity with error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(HashComputationException.class)
    public ResponseEntity<ErrorResponse> handleHashComputationException(HashComputationException ex) {
        log.error("Hash computation error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                "HASH_COMPUTATION_ERROR",
                "An error occurred during hash computation",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handles JwtValidationException and returns appropriate HTTP response.
     *
     * @param ex The JwtValidationException that was thrown
     * @return ResponseEntity with error details and UNAUTHORIZED status
     */
    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<ErrorResponse> handleJwtValidationException(JwtValidationException ex) {
        log.warn("JWT validation failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "JWT_VALIDATION_ERROR",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Handles any unhandled exceptions and returns appropriate HTTP response.
     *
     * @param ex The Exception that was thrown
     * @return ResponseEntity with error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
