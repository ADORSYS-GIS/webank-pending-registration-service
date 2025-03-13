package com.adorsys.webank.dto;

public class OtpValidationRequest {
    private String phoneNumber;
    private String otpInput;
    private String otpHash;

    public OtpValidationRequest() {}

    public OtpValidationRequest(String phoneNumber, String otpInput , String otpHash) {
        this.phoneNumber = phoneNumber;
        this.otpInput = otpInput;
        this.otpHash = otpHash;
    }



    public String getPhoneNumber() {
        return phoneNumber;
    }
    public String getOtpHash() {
        return otpHash;
    }
    public String getOtpInput() {
        return otpInput;
    }




    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }
    public void setOtpInput(String otpInput) {
        this.otpInput = otpInput;
    }
}