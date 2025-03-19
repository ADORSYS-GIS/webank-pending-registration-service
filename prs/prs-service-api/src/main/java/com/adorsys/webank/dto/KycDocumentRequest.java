package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycDocumentRequest {
    private byte[] frontId;
    private byte[] backId;
    private byte[] taxId;
    private byte[] selfieId;

    public KycDocumentRequest(byte[] frontId, byte[] backId, byte[] taxId, byte[] selfieId) {
        this.frontId = frontId;
        this.backId = backId;
        this.taxId = taxId;
        this.selfieId = selfieId;
    }

}
