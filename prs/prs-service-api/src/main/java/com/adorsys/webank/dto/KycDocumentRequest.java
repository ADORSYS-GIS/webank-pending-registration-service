package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycDocumentRequest {
    private String frontId;
    private String backId;
    private String taxId;
    private String selfieId;

    public KycDocumentRequest(String frontId, String backId, String taxId, String selfieId) {
        this.frontId = frontId;
        this.backId = backId;
        this.taxId = taxId;
        this.selfieId = selfieId;
    }
}
