package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response")
public class ErrorResponse {
    
    @Schema(description = "Error code", example = "INVALID_OTP", required = true)
    private String code;
    
    @Schema(description = "Human-readable error message", example = "The provided OTP is invalid or has expired", required = true)
    private String message;
    
    @Schema(description = "Detailed error information for debugging", example = "OTP validation failed: code does not match")
    private String details;
    
    @Schema(description = "Timestamp when the error occurred", example = "2025-01-20T15:30:00")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Schema(description = "Request path that caused the error", example = "/api/prs/otp/validate")
    private String path;
    
    // Factory methods
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, LocalDateTime.now(), null);
    }
    
    public static ErrorResponse of(String code, String message, String details) {
        return new ErrorResponse(code, message, details, LocalDateTime.now(), null);
    }
} 