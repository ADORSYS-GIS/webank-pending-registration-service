package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.KycProcessingException;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.*;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class KycRecoveryServiceImpl implements KycRecoveryServiceApi {

    private final PersonalInfoRepository inforepository;

    public KycRecoveryServiceImpl(PersonalInfoRepository inforepository) {
        this.inforepository = inforepository;
    }

    @Override
    @Transactional
    public String verifyKycRecoveryFields(String accountId, String idNumber, String expiryDate) {
        // Validate input parameters
        validateInputParameters(accountId, idNumber, expiryDate);
        
        // Find and validate personal info
        PersonalInfoEntity personalInfo = findPersonalInfoByAccountId(accountId);
        
        // Validate document details
        validateDocumentDetails(personalInfo, accountId, idNumber, expiryDate);

        // If all validations pass
        log.info("Document verification successful for accountId {}", accountId);
        return "Document verification successful";
    }
    
    /**
     * Validates that input parameters are not null or empty
     */
    private void validateInputParameters(String accountId, String idNumber, String expiryDate) {
        validateNotEmpty(accountId, "Account ID");
        validateNotEmpty(idNumber, "Document ID");
        validateNotEmpty(expiryDate, "Expiry date");
    }

    /**
     * Helper method to validate a string parameter is not null or empty
     * @param value The string value to check
     * @param fieldName Name of the field for the error message
     * @throws IllegalArgumentException if value is null or empty
     */
    private void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
    
    /**
     * Finds personal info by account ID or throws an exception if not found
     */
    private PersonalInfoEntity findPersonalInfoByAccountId(String accountId) {
        return inforepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    log.warn("No record found for accountId {}", accountId);
                    return new KycProcessingException("No record found for accountId " + accountId);
                });
    }
    
    /**
     * Validates document details match with stored personal info
     */
    private void validateDocumentDetails(PersonalInfoEntity personalInfo, String accountId, 
                                      String idNumber, String expiryDate) {
        if (!personalInfo.getDocumentUniqueId().equals(idNumber)) {
            log.warn("Document ID mismatch for accountId {}", accountId);
            throw new KycProcessingException("Document ID mismatch");
        }

        if (!personalInfo.getExpirationDate().equals(expiryDate)) {
            log.warn("Document expiry date mismatch for accountId {}", accountId);
            throw new KycProcessingException("Document expiry date mismatch");
        }
    }
}