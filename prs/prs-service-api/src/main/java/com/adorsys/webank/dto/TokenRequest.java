package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRequest {
    private String newAccountId;
    private String oldAccountId;

    public TokenRequest(String newAccountId, String oldAccountId) {
        this.newAccountId = newAccountId;
        this.oldAccountId = oldAccountId;
    }
}