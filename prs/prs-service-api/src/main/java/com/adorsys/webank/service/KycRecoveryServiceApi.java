package com.adorsys.webank.service;
import org.springframework.stereotype.Service;

@Service
public interface KycRecoveryServiceApi {
    /**
     * Verifies the document ID and expiration date provided by the user during the KYC recovery process.
     *
     * @param accountId The account ID of the user.
     * @param idNumber  The document ID number provided by the user.
     * @param expiryDate The expiration date of the document.
     * @return A string indicating the result of the verification process.
     */
    String verifyKycRecoveryFields(String accountId, String idNumber, String expiryDate);
}
