package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import lombok.Builder;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "OTP generation response")
public class OtpResponse {
    
    @Schema(description = "Hashed OTP identifier for validation", example = "a1b2c3d4e5f6", required = true)
    private String otpHash;
    
    @Schema(description = "Phone number where OTP was sent", example = "+237691234567", required = true)
    private String phoneNumber;
    
    @Schema(description = "OTP expiration time", example = "2025-01-20T15:35:00", required = true)
    private LocalDateTime expiresAt;
    
    @Schema(description = "OTP validity duration in seconds", example = "300")
    private Integer validitySeconds = 300;
    
    @Schema(description = "Indicates if OTP was sent successfully", example = "true")
    private boolean sent = true;
} 