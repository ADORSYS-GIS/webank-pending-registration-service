package com.adorsys.webank.dto;

public class KycLocationRequest {
    private String location;

    public KycLocationRequest(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
