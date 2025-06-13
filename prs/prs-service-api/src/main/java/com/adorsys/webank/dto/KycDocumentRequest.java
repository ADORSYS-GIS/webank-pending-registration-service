package com.adorsys.webank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC document submission request containing document identifiers")
public class KycDocumentRequest {
    
    @Schema(description = "Front side of ID document identifier/URL", 
            example = "doc_front_123456", 
            required = true)
    private String frontId;
    
    @Schema(description = "Back side of ID document identifier/URL", 
            example = "doc_back_123456", 
            required = true)
    private String backId;
    
    @Schema(description = "Tax document identifier/URL", 
            example = "doc_tax_123456")
    private String taxId;
    
    @Schema(description = "Selfie photo identifier/URL for identity verification", 
            example = "doc_selfie_123456", 
            required = true)
    private String selfieId;

    @Schema(description = "User's account identifier", 
            example = "ACC_1234567890", 
            required = true)
    private String accountId;
}