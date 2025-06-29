package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycRecoveryServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycRecoveryServiceImpl implements KycRecoveryServiceApi {

    private final PersonalInfoRepository inforepository;

    @Override
    @Transactional
    public String verifyKycRecoveryFields(String accountId, String idNumber, String expiryDate) {
        String correlationId = MDC.get("correlationId");
        log.info("Verifying KYC recovery fields for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        log.debug("Verifying with ID: {}, expiry date: {} [correlationId={}]", 
                maskIdNumber(idNumber), expiryDate, correlationId);
        
        Optional<PersonalInfoProjection> personalInfoOpt = inforepository.findByAccountId(accountId);

        if (personalInfoOpt.isEmpty()) {
            log.warn("No record found for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            return "Failed: No record found for accountId " + accountId;
        }

        PersonalInfoProjection personalInfo = personalInfoOpt.get();
        log.debug("Found personal info record for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);

        // Validate document details
        if (!personalInfo.getDocumentUniqueId().equals(idNumber)) {
            log.warn("Document ID mismatch for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            return "Failed: Document ID mismatch";
        }

        if (!personalInfo.getExpirationDate().equals(expiryDate)) {
            log.warn("Document expiry date mismatch for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            return "Failed: Document expiry date mismatch";
        }

        // If all validations pass
        log.info("Document verification successful for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        return "Document verification successful";
    }
    
    /**
     * Masks an account ID for logging purposes
     * Shows only first 2 and last 2 characters
     */
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 5) {
            return "********";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
    
    /**
     * Masks an ID number for logging purposes
     * Shows only first 2 and last 2 characters
     */
    private String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 5) {
            return "********";
        }
        return idNumber.substring(0, 2) + "****" + idNumber.substring(idNumber.length() - 2);
    }
}