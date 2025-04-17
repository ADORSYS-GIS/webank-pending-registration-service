package com.adorsys.webank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String accountId;
    private String idNumber;
    private String expirationDate;
    private String location;
    private String email;
    private String status;
    private String frontID;
    private String backID;
    private String selfie;
    private String taxDocument;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFrontID() {
        return frontID;
    }

    public void setFrontID(String frontID) {
        this.frontID = frontID;
    }

    public String getBackID() {
        return backID;
    }

    public void setBackID(String backID) {
        this.backID = backID;
    }

    public String getSelfie() {
        return selfie;
    }

    public void setSelfie(String selfie) {
        this.selfie = selfie;
    }

    public String getTaxDocument() {
        return taxDocument;
    }

    public void setTaxDocument(String taxDocument) {
        this.taxDocument = taxDocument;
    }
}