package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KycEmailRequest {
    private String email;
    private String accountId;

    public KycEmailRequest(String email , String accountId) {
        this.email = email;
        this.accountId = accountId;
    }

}
