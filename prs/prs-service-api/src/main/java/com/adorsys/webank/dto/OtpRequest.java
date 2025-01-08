package com.adorsys.webank.dto;

public class OtpRequest {
    private String phoneNumber;

    public OtpRequest() {}

    public OtpRequest(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
