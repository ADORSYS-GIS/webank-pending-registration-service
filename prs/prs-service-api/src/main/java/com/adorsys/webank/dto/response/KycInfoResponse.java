package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC personal information submission response")
public class KycInfoResponse {
    
    @Schema(description = "KYC processing status", example = "PENDING", required = true)
    private KycResponse.KycStatus status;
    
    @Schema(description = "Submission timestamp", example = "2025-01-20T15:30:00")
    private LocalDateTime submittedAt;
    
    @Schema(description = "Additional processing message", example = "KYC information submitted successfully")
    private String message;
    
    @Schema(description = "Account ID associated with the personal information", example = "ACC_1234567890")
    private String accountId;
    
    @Schema(description = "Document unique ID (national ID, passport, etc.)", example = "ID123456789")
    private String idNumber;
    
    @Schema(description = "Document expiry date", example = "2025-12-31")
    private String expiryDate;
    
    @Schema(description = "Verification status", example = "VALID")
    private VerificationStatus verificationStatus;
    
    @Schema(description = "Reason for rejection if applicable", example = "Document expired")
    private String rejectionReason;
    
    public enum VerificationStatus {
        @Schema(description = "Personal information pending verification")
        PENDING,
        @Schema(description = "Personal information validated successfully")
        VALID,
        @Schema(description = "Personal information rejected")
        REJECTED,
        @Schema(description = "Document expired")
        EXPIRED,
        @Schema(description = "Additional information required")
        REQUIRES_MORE_INFO
    }
} 