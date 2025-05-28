package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC email submission response")
public class KycEmailResponse {
    
    @Schema(description = "Unique KYC email record identifier", example = "kyc_email_1234567890", required = true)
    private String kycId;
    
    @Schema(description = "KYC processing status", example = "PENDING", required = true)
    private KycResponse.KycStatus status;
    
    @Schema(description = "Submission timestamp", example = "2025-01-20T15:30:00")
    private LocalDateTime submittedAt;
    
    @Schema(description = "Additional processing message", example = "Email verification sent to user@example.com")
    private String message;
    
    @Schema(description = "Account ID associated with the email", example = "ACC_1234567890")
    private String accountId;
    
    @Schema(description = "Email address", example = "user@example.com")
    private String email;
    
    @Schema(description = "Email verification status", example = "VERIFICATION_SENT")
    private VerificationStatus verificationStatus;
    
    @Schema(description = "Time when verification was completed, if applicable", example = "2025-01-20T15:35:00")
    private LocalDateTime verifiedAt;
    
    public enum VerificationStatus {
        @Schema(description = "Email pending verification")
        PENDING,
        @Schema(description = "Email verification sent")
        VERIFICATION_SENT,
        @Schema(description = "Email verification reminder sent")
        REMINDER_SENT,
        @Schema(description = "Email verified successfully")
        VERIFIED,
        @Schema(description = "Email verification failed")
        FAILED,
        @Schema(description = "Email verification expired")
        EXPIRED
    }
} 