package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC submission response")
public class KycResponse {
    
    @Schema(description = "Unique KYC record identifier", example = "kyc_1234567890", required = true)
    private String kycId;
    
    @Schema(description = "KYC processing status", example = "PENDING", required = true)
    private KycStatus status;
    
    @Schema(description = "Submission timestamp", example = "2025-01-20T15:30:00")
    private LocalDateTime submittedAt;
    
    @Schema(description = "Additional processing message", example = "KYC documents submitted successfully")
    private String message;
    
    @Schema(description = "Document verification details")
    private Object verificationDetails;
    
    public enum KycStatus {
        @Schema(description = "KYC is pending review")
        PENDING,
        @Schema(description = "KYC is under review")
        IN_REVIEW,
        @Schema(description = "KYC approved")
        APPROVED,
        @Schema(description = "KYC rejected")
        REJECTED,
        @Schema(description = "Additional information required")
        REQUIRES_INFO
    }
} 