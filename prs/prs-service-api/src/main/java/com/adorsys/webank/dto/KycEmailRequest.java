package com.adorsys.webank.dto;

public class KycEmailRequest {
    private String email;

    public KycEmailRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
