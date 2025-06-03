package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.KycProcessingException;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.adorsys.webank.projection.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;



@Service
@Slf4j
public class KycStatusUpdateServiceImpl implements KycStatusUpdateServiceApi {

    private final PersonalInfoRepository inforepository;

    public KycStatusUpdateServiceImpl(PersonalInfoRepository inforepository) {
        this.inforepository = inforepository;
    }

    @Override
    @Transactional
    public String updateKycStatus(String accountId, String newStatus, String idNumber, String expiryDate, String rejectionReason) {
        if (log.isInfoEnabled()) {
            log.info("Updating KYC status for accountId {} to {} with rejection reason: {}",
                    accountId, newStatus, rejectionReason);
        }

        // Validate all input parameters
        validateInputParameters(accountId, newStatus, idNumber, expiryDate);
        
        // Find and validate personal info record
        PersonalInfoProjection personalInfo = findAndValidatePersonalInfo(accountId, idNumber, expiryDate);

        // Process the status update
        return processStatusUpdate(personalInfo, accountId, newStatus, rejectionReason);
    }
    
    /**
     * Validates that all input parameters are not null or empty
     */
    private void validateInputParameters(String accountId, String newStatus, String idNumber, String expiryDate) {
        validateNotEmpty(accountId, "Account ID");
        validateNotEmpty(newStatus, "New status");
        validateNotEmpty(idNumber, "Document ID");
        validateNotEmpty(expiryDate, "Expiry date");
    }

    /**
     * Find entity by account ID for updates
     */
    private PersonalInfoEntity findEntityByAccountId(String accountId) {
        // Use native query or JPA query to get the full entity, not just the projection
        return inforepository.findAll().stream()
               .filter(entity -> entity.getAccountId().equals(accountId))
               .findFirst()
               .orElseThrow(() -> new KycProcessingException("Entity not found for accountId " + accountId));
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
     * Finds personal info by account ID and validates document details
     */
    private PersonalInfoProjection findAndValidatePersonalInfo(String accountId, String idNumber, String expiryDate) {
        // Find the personal info by accountId
        PersonalInfoProjection personalInfo = inforepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    if (log.isWarnEnabled()) {
                        log.warn("No record found for accountId {}", accountId);
                    }
                    return new KycProcessingException("No record found for accountId " + accountId);
                });

        // Validate document details
        validateDocumentDetails(personalInfo, accountId, idNumber, expiryDate);
        
        return personalInfo;
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
    
    /**
     * Process the status update including validation and persistence
     */
    private String processStatusUpdate(PersonalInfoProjection projection, String accountId, 
                                   String newStatus, String rejectionReason) {
        try {
            // Convert newStatus string to Enum
            PersonalInfoStatus kycStatus = PersonalInfoStatus.valueOf(newStatus.toUpperCase());
            
            // Fetch the actual entity for updates using accountId
            PersonalInfoEntity personalInfo = findEntityByAccountId(projection.getAccountId());
            
            // Update the status and handle rejection fields
            updateStatusAndRejectionFields(personalInfo, kycStatus, rejectionReason);
            
            // Save the changes
            inforepository.save(personalInfo);

            if (log.isInfoEnabled()) {
                log.info("Successfully updated KYC status for accountId {}", accountId);
            }
            return "KYC status for " + accountId + " updated to " + newStatus;
        } catch (IllegalArgumentException e) {
            if (log.isErrorEnabled()) {
                log.error("Invalid KYC status value: {}", newStatus);
            }
            throw new KycProcessingException("Invalid KYC status value '" + newStatus + "'", e);
        }
    }
    
    /**
     * Update the status and handle rejection fields based on the new status
     */
    private void updateStatusAndRejectionFields(PersonalInfoEntity personalInfo, 
                                           PersonalInfoStatus kycStatus, String rejectionReason) {
        // Update status field
        personalInfo.setStatus(kycStatus);
        
        // Handle rejection reason based on status
        if (kycStatus == PersonalInfoStatus.REJECTED) {
            validateRejectionReason(rejectionReason);
            personalInfo.setRejectionReason(rejectionReason);
        } else {
            // Clear rejection fields if status is not REJECTED
            personalInfo.setRejectionReason(null);
        }
    }
    
    /**
     * Validate rejection reason when status is REJECTED
     */
    private void validateRejectionReason(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new KycProcessingException("Rejection reason is required when status is REJECTED");
        }
    }
}
