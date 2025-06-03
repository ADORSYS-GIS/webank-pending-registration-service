package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.exceptions.KycProcessingException;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycRecoveryServiceApi;
import com.adorsys.webank.projection.PersonalInfoProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
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
        // Validate input parameters directly
        validateNotEmpty(accountId, "Account ID");
        validateNotEmpty(idNumber, "Document ID");
        validateNotEmpty(expiryDate, "Expiry date");
        
        // Find and validate personal info
        PersonalInfoProjection personalInfo = findPersonalInfoByAccountId(accountId);

        // Validate document details
        validateDocumentDetails(personalInfo, accountId, idNumber, expiryDate);

        // If all validations pass
        if (log.isInfoEnabled()) {
            log.info("Document verification successful for accountId {}", accountId);
        }
        return "Document verification successful";
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
    private PersonalInfoProjection findPersonalInfoByAccountId(String accountId) {
        return inforepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    if (log.isWarnEnabled()) {
                        log.warn("No record found for accountId {}", accountId);
                    }
                    return new KycProcessingException("No record found for accountId " + accountId);
                });
    }
    
    /**
     * Validates document details match with stored personal info
     */
    private void validateDocumentDetails(PersonalInfoProjection personalInfo, String accountId, 
                                      String idNumber, String expiryDate) {
        if (!personalInfo.getDocumentUniqueId().equals(idNumber)) {
            if (log.isWarnEnabled()) {
                log.warn("Document ID mismatch for accountId {}", accountId);
            }
            throw new KycProcessingException("Document ID mismatch");
        }

        if (!personalInfo.getExpirationDate().equals(expiryDate)) {
            if (log.isWarnEnabled()) {
                log.warn("Document expiry date mismatch for accountId {}", accountId);
            }
            throw new KycProcessingException("Document expiry date mismatch");
        }
    }
}