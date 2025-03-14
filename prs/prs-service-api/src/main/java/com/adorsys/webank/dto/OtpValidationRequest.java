package com.adorsys.webank.dto;

public class OtpValidationRequest {
    private String phoneNumber;
    private String otpInput;

    public OtpValidationRequest() {}

    public OtpValidationRequest(String phoneNumber, String otpInput) {
        this.phoneNumber = phoneNumber;
        this.otpInput = otpInput;
    }



    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getOtpInput() {
        return otpInput;
    }




    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setOtpInput(String otpInput) {
        this.otpInput = otpInput;
    }
}
