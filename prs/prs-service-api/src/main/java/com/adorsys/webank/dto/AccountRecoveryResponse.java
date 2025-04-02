package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AccountRecoveryResponse {
    // Getters and Setters
    private String oldAccountId;
    private String newKycCertificate;
    private String message;

    public AccountRecoveryResponse(String oldAccountId, String newKycCertificate, String message) {
        this.oldAccountId = oldAccountId;
        this.newKycCertificate = newKycCertificate;
        this.message = message;
    }

}
