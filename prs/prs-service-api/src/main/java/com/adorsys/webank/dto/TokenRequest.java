package com.adorsys.webank.dto;

public class TokenRequest {
    private String newAccountId;
    private String oldAccountId;

    public TokenRequest(String newAccountId, String oldAccountId) {
        this.newAccountId = newAccountId;
        this.oldAccountId = oldAccountId;
    }

    public String getNewAccountId() {
        return newAccountId;
    }

    public void setNewAccountId(String newAccountId) {
        this.newAccountId = newAccountId;
    }

    public String getOldAccountId() {
        return oldAccountId;
    }

    public void setOldAccountId(String oldAccountId) {
        this.oldAccountId = oldAccountId;
    }
}