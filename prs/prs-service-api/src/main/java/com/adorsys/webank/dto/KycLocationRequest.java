package com.adorsys.webank.dto;

public class KycLocationRequest {
    private String location;
    private String accountId;

    public KycLocationRequest(String location, String accountId) {
        this.location = location;
        this.accountId = accountId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}