package com.adorsys.webank.dto;

public class KycGetDocRequest {
    private String publicKeyHash;

    public KycGetDocRequest() {
    }

    public KycGetDocRequest(String publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }

    public String getPublicKeyHash() {
        return publicKeyHash;
    }

    public void setPublicKeyHash(String publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }
}
