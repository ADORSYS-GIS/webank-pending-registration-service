package com.adorsys.webank.dto;

public class OtpRequest {
    private String phoneNumber;
    private String publicKey;

    public OtpRequest(String phoneNumber , String publicKey) {
        this.phoneNumber = phoneNumber;
        this.publicKey = publicKey;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public String getPublicKey() {
        return publicKey;
    }


    public void setPhoneNumber(String phoneNumber ) {
        this.phoneNumber = phoneNumber;
    }
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
