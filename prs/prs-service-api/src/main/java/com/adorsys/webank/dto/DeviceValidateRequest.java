package com.adorsys.webank.dto;


public class DeviceValidateRequest {

    private String initiationNonce;
    private String powHash;
    private String powNonce;


    public String getInitiationNonce() {
        return initiationNonce;
    }

    public void setInitiationNonce(String initiationNonce) {
        this.initiationNonce = initiationNonce;
    }

    public String getPowHash() {
        return powHash;
    }

    public void setPowHash(String powHash) {
        this.powHash = powHash;
    }

    public String getPowNonce() {
        return powNonce;
    }

    public void setPowNonce(String powNonce) {
        this.powNonce = powNonce;
    }



}
