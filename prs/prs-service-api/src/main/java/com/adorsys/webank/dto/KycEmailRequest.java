package com.adorsys.webank.dto;

public class KycEmailRequest {
    private String email;
    private String accountId;

    public KycEmailRequest(String email , String accountId) {
        this.email = email;
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
