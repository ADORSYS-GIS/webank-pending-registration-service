package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TokenRequest {
    private String newAccountId;
    private String oldAccountId;

    public TokenRequest(String newAccountId, String oldAccountId) {
        this.newAccountId = newAccountId;
        this.oldAccountId = oldAccountId;
    }
}