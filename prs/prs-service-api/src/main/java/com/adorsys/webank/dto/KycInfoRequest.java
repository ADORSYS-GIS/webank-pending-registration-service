package com.adorsys.webank.dto;

public class KycInfoRequest {

    private String idNumber;
    private String expiryDate;
    private String accountId;

    public KycInfoRequest(String idNumber, String expiryDate , String accountId) {

        this.idNumber = idNumber;
        this.expiryDate = expiryDate;
        this.accountId = accountId;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
