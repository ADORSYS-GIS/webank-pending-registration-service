package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object for email validation operations")
public class EmailValidationResponse {

    @Schema(description = "Status of the email validation", example = "VALID")
    private ValidationStatus status;

    @Schema(description = "Timestamp when the validation was performed", example = "2025-01-20T15:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Response message with details about the validation", example = "Email validated successfully")
    private String message;

    public enum ValidationStatus {
        SUCCESS,
        FAILED
    }
}
