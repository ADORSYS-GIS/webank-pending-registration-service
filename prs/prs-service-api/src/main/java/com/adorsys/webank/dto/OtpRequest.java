package com.adorsys.webank.dto;

public class OtpRequest {
    private String phoneNumber;

    public OtpRequest(String phoneNumber , String publicKey) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }


    public void setPhoneNumber(String phoneNumber ) {
        this.phoneNumber = phoneNumber;
    }
}
