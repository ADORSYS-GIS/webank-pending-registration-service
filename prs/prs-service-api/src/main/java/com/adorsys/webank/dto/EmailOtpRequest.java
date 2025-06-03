package com.adorsys.webank.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmailOtpRequest {
    private String email;
    private String accountId;

    public EmailOtpRequest(String email, String accountId) {
        this.email = email;
        this.accountId = accountId;
    }
}
