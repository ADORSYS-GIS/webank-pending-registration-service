package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation response for OTP or other validation operations")
public class OtpValidationResponse {
    
    @Schema(description = "Indicates if validation was successful", example = "true", required = true)
    private boolean valid;
    
    @Schema(description = "Validation message", example = "OTP validated successfully")
    private String message;
    
    @Schema(description = "Additional validation details")
    private Object details;
} 