package com.adorsys.webank.dto;

public class DeviceValidateRequest {

    private String initiationNonce;
    private String powHash;

    public DeviceValidateRequest(String initiationNonce , String powHash) {
        this.initiationNonce = initiationNonce;
        this.powHash = powHash;
    }

    public String getinitiationNonce() {
        return initiationNonce;
    }
    public String powHash() {
        return powHash;
    }


    public void setinitiationNonce(String initiationNonce ) {
        this.initiationNonce = DeviceValidateRequest.this.initiationNonce;
    }
    public void setpowHash(String powHash) {
        this.powHash = DeviceValidateRequest.this.powHash;
    }
}
