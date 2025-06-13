package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC document submission response")
public class KycDocumentResponse {
    
    @Schema(description = "Unique KYC document record identifier", example = "kyc_doc_1234567890", required = true)
    private String kycId;
    
    @Schema(description = "KYC processing status", example = "PENDING", required = true)
    private KycResponse.KycStatus status;
    
    @Schema(description = "Submission timestamp", example = "2025-01-20T15:30:00")
    private LocalDateTime submittedAt;
    
    @Schema(description = "Additional processing message", example = "KYC documents submitted successfully")
    private String message;
    
    @Schema(description = "List of document statuses", required = false)
    private List<DocumentStatus> documentStatuses;
    
    @Schema(description = "Account ID associated with the documents", example = "ACC_1234567890")
    private String accountId;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentStatus {
        @Schema(description = "Document type", example = "FRONT_ID")
        private DocumentType documentType;
        
        @Schema(description = "Document verification status", example = "VERIFIED")
        private VerificationStatus status;
        
        @Schema(description = "Document verification notes", example = "Document image is clear")
        private String notes;
    }
    
    public enum DocumentType {
        @Schema(description = "Front side of ID document")
        FRONT_ID,
        @Schema(description = "Back side of ID document")
        BACK_ID,
        @Schema(description = "Tax document")
        TAX_DOCUMENT,
        @Schema(description = "Selfie photo")
        SELFIE
    }
    
    public enum VerificationStatus {
        @Schema(description = "Document pending verification")
        PENDING,
        @Schema(description = "Document verified successfully")
        VERIFIED,
        @Schema(description = "Document rejected")
        REJECTED,
        @Schema(description = "Document unclear or unreadable")
        UNCLEAR
    }
} 