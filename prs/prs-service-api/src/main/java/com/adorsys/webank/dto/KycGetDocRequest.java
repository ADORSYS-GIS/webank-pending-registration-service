package com.adorsys.webank.dto;

public class KycGetDocRequest {
    private String accountId;

    public KycGetDocRequest() {
    }

    public KycGetDocRequest(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}