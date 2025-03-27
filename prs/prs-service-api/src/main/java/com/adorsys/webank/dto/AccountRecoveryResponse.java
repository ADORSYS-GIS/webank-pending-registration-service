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
    private String newAccountCertificate;

    public AccountRecoveryResponse(String oldAccountId, String newKycCertificate, String newAccountCertificate, String message) {
        this.oldAccountId = oldAccountId;
        this.newKycCertificate = newKycCertificate;
        this.message = message;
        this.newAccountCertificate = newAccountCertificate;
    }

}
