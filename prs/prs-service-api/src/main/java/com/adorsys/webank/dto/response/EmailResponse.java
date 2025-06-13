package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object for email operations")
public class EmailResponse {
    
    @Schema(description = "Status of the email operation", example = "SUCCESS")
    private EmailStatus status;
    
    @Schema(description = "Timestamp when the operation was performed", example = "2025-01-20T15:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "Response message with details about the operation", example = "Email OTP sent successfully")
    private String message;

    public enum EmailStatus {
        SUCCESS,
        PENDING,
        FAILED
    }
} 