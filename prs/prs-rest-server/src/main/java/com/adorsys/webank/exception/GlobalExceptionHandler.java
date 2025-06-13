package com.adorsys.webank.exception;

import com.adorsys.webank.dto.response.ErrorResponse;
import com.adorsys.webank.exceptions.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;


@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Begin merged business exception handlers ---
    @ExceptionHandler(OtpValidationException.class)
    public ResponseEntity<ErrorResponse> handleOtpValidationException(OtpValidationException ex, WebRequest request) {
        log.warn("OTP validation failed: {}", ex.getMessage());
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                "OTP_VALIDATION_ERROR",
                ex.getMessage(),
                null,
                LocalDateTime.now(),
                path
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HashComputationException.class)
    public ResponseEntity<ErrorResponse> handleHashComputationException(HashComputationException ex, WebRequest request) {
        log.error("Hash computation error: {}", ex.getMessage(), ex);
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                "HASH_COMPUTATION_ERROR",
                "An error occurred during hash computation",
                ex.getMessage(),
                LocalDateTime.now(),
                path
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(KycProcessingException.class)
    public ResponseEntity<ErrorResponse> handleKycProcessingException(KycProcessingException ex, WebRequest request) {
        log.warn("KYC processing error: {}", ex.getMessage());
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                "KYC_PROCESSING_ERROR",
                ex.getMessage(),
                null,
                LocalDateTime.now(),
                path
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(OtpProcessingException.class)
    public ResponseEntity<ErrorResponse> handleOtpProcessingException(OtpProcessingException ex, WebRequest request) {
        log.error("OTP processing error: {}", ex.getMessage(), ex);
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                "OTP_PROCESSING_ERROR",
                ex.getMessage(),
                null,
                LocalDateTime.now(),
                path
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(AccountNotFoundException ex, WebRequest request) {
        log.warn("Account not found: {}", ex.getMessage());
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                "ACCOUNT_NOT_FOUND",
                ex.getMessage(),
                null,
                LocalDateTime.now(),
                path
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    // --- End merged business exception handlers ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ApiResponse(
        responseCode = "400",
        description = "Validation error",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                errors,
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception occurred", ex);
        
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR", 
            "An unexpected error occurred",
            ex.getMessage(),
            LocalDateTime.now(),
            path
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(InvalidRequestException ex, WebRequest request) {
        log.error("Invalid request", ex);
        
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_REQUEST", 
            "Invalid request parameters",
            ex.getMessage(),
            LocalDateTime.now(),
            path
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.error("Authentication failed", ex);
        
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
            "UNAUTHORIZED", 
            "Authentication failed",
            ex.getMessage(),
            LocalDateTime.now(),
            path
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex, WebRequest request) {
        log.error("Security exception", ex);
        
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
            "UNAUTHORIZED", 
            "Authentication failed",
            ex.getMessage(),
            LocalDateTime.now(),
            path
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.error("Invalid argument", ex);
        
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_REQUEST", 
            "Invalid request parameters",
            ex.getMessage(),
            LocalDateTime.now(),
            path
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ApiResponse(
        responseCode = "401",
        description = "Missing required header",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex,
            WebRequest request) {
        
        String headerName = ex.getHeaderName();
        ErrorResponse errorResponse = new ErrorResponse(
                "MISSING_AUTH_HEADER",
                "Missing required header: " + headerName,
                "Please provide the " + headerName + " header with a valid JWT token in the format: Bearer <token>",
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
} 