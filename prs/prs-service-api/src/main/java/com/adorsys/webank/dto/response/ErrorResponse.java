package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response for API errors")
public class ErrorResponse {
    
    @Schema(description = "Error code", example = "INVALID_REQUEST", required = true)
    private String code;
    
    @Schema(description = "Error message", example = "Invalid request parameters", required = true)
    private String message;
    
    @Schema(description = "Detailed error description", example = "Phone number format is invalid")
    private String details;
    
    @Schema(description = "Timestamp when the error occurred", example = "2025-01-20T15:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "API endpoint path where the error occurred", example = "/api/prs/otp/send")
    private String path;
    
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(String code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
} 