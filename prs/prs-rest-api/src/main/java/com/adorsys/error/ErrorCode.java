package com.adorsys.error;

public enum ErrorCode {
    // Client Errors (4xx)
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource Not Found"),
    VALIDATION_ERROR(422, "Validation Error"),
    
    // Server Errors (5xx)
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    
    // Business Logic Errors
    INVALID_REGISTRATION(400, "Invalid Registration Data"),
    DUPLICATE_REGISTRATION(409, "Duplicate Registration"),
    INVALID_STATUS(400, "Invalid Status"),
    INVALID_OPERATION(400, "Invalid Operation"),
    
    // Device Registration Specific Errors
    INVALID_JWT(401, "Invalid JWT Token"),
    INVALID_AUTHORIZATION_HEADER(401, "Invalid Authorization Header"),
    INVALID_TIMESTAMP(400, "Invalid Timestamp"),
    INVALID_NONCE(400, "Invalid Nonce"),
    INVALID_POW_HASH(400, "Invalid Proof of Work Hash"),
    DEVICE_REGISTRATION_FAILED(400, "Device Registration Failed"),
    DEVICE_VALIDATION_FAILED(400, "Device Validation Failed"),
    ACCOUNT_NOT_FOUND(404, "Account not found"),
    HASH_COMPUTATION_ERROR(500, "Error computing hash"),
    OTP_SEND_ERROR(500, "Failed to send OTP"),
    KYC_PROCESSING_ERROR(500, "Error processing KYC request");

    private final int httpStatus;
    private final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
} 