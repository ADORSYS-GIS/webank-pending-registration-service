package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailOtpRequest {
    private String email;
    private String accountId;

    public EmailOtpRequest(String email, String accountId) {
        this.email = email;
        this.accountId = accountId;
    }
}
