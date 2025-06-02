package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KycDocumentRequest {
    private String frontId;
    private String backId;
    private String taxId;
    private String selfieId;

    private String accountId;

    public KycDocumentRequest(String frontId, String backId, String taxId, String selfieId, String accountId) {
        this.frontId = frontId;
        this.backId = backId;
        this.taxId = taxId;
        this.selfieId = selfieId;
        this.accountId = accountId;
    }
}