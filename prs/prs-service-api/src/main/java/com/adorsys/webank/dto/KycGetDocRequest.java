package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycGetDocRequest {
    private String accountId;

    public KycGetDocRequest() {
    }

}