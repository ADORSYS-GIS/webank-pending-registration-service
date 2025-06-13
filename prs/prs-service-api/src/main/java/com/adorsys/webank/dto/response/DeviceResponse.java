package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object for device registration initialization")
public class DeviceResponse {
    
    @Schema(description = "Status of the device registration initialization", example = "INITIALIZED")
    private InitStatus status;
    
    @Schema(description = "Timestamp when the operation was performed", example = "2025-01-20T15:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "Initialization response message with information",
            example = "Device registration initialized.")
    private String message;

    @Schema(description = "Unique nonce generated during device registration initialization",
            example = "abc123def456")
    private String nonce;

    public enum InitStatus {
        INITIALIZED,
        FAILED
    }
} 