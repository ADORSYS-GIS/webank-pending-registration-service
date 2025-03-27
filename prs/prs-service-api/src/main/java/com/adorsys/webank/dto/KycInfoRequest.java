package com.adorsys.webank.dto;

public class KycInfoRequest {
    private String fullName;
    private String profession;
    private String idNumber;
    private String dateOfBirth;
    private String currentRegion;
    private String expiryDate;
    private String accountId;  // Added accountId field

    public KycInfoRequest(String fullName, String profession, String idNumber, String dateOfBirth, String currentRegion, String expiryDate, String accountId) {
        this.fullName = fullName;
        this.profession = profession;
        this.idNumber = idNumber;
        this.dateOfBirth = dateOfBirth;
        this.currentRegion = currentRegion;
        this.expiryDate = expiryDate;
        this.accountId = accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCurrentRegion() {
        return currentRegion;
    }

    public void setCurrentRegion(String currentRegion) {
        this.currentRegion = currentRegion;
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
