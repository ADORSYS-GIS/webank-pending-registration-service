package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object for device validation operations")
public class DeviceValidationResponse {
    
    @Schema(description = "Status of the device validation", example = "VALIDATED")
    private ValidationStatus status;
    
    @Schema(description = "Timestamp when the validation was performed", example = "2025-01-20T15:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "Device certificate JWT token", 
            example = "eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJKV1QifQ...")
    private String certificate;
    
    @Schema(description = "Additional information about the validation process", 
            example = "Device successfully validated and certificate issued")
    private String message;

    public enum ValidationStatus {
        VALIDATED,
        REJECTED,
        FAILED
    }
} 