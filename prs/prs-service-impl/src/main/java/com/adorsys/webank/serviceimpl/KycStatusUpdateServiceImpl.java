package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import org.slf4j.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

@Service
public class KycStatusUpdateServiceImpl implements KycStatusUpdateServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycStatusUpdateServiceImpl.class);
    private final PersonalInfoRepository inforepository;

    public KycStatusUpdateServiceImpl(PersonalInfoRepository inforepository) {
        this.inforepository = inforepository;
    }

    @Override
    @Transactional
    public String updateKycStatus(String accountId, String newStatus, String idNumber, String expiryDate, String rejectionReason) {
        log.info("Updating KYC status for account: {} to status: {}", 
                maskAccountId(accountId), newStatus);
        
        if (rejectionReason != null && !rejectionReason.isEmpty()) {
            log.debug("Rejection reason provided: {}", rejectionReason);
        }

        Optional<PersonalInfoEntity> personalInfoOpt = inforepository.findByAccountId(accountId);
        if (!personalInfoOpt.isPresent()) {
            log.warn("No record found for account: {}", maskAccountId(accountId));
            return "Failed: No record found for accountId " + maskAccountId(accountId);
        }
        
        PersonalInfoEntity personalInfo = personalInfoOpt.get();
        log.debug("Found personal info record for account: {}", maskAccountId(accountId));

        // Validate document details
        String validationResult = validateDocumentDetails(accountId, personalInfo, idNumber, expiryDate);
        if (validationResult != null) {
            return validationResult;
        }

        return processStatusUpdate(accountId, newStatus, rejectionReason, personalInfo);
    }
    
    /**
     * Validates the document details match what's in the database
     */
    private String validateDocumentDetails(String accountId, PersonalInfoEntity personalInfo, 
                                          String idNumber, String expiryDate) {
        if (!personalInfo.getDocumentUniqueId().equals(idNumber)) {
            log.warn("Document ID mismatch for account: {}", maskAccountId(accountId));
            return "Failed: Document ID mismatch";
        }

        if (!personalInfo.getExpirationDate().equals(expiryDate)) {
            log.warn("Document expiry date mismatch for account: {}", maskAccountId(accountId));
            return "Failed: Document expiry date mismatch";
        }
        
        return null; // Validation passed
    }
    
    /**
     * Processes the status update after validation passes
     */
    private String processStatusUpdate(String accountId, String newStatus, 
                                      String rejectionReason, PersonalInfoEntity personalInfo) {
        try {
            // Convert newStatus string to Enum
            PersonalInfoStatus kycStatus = PersonalInfoStatus.valueOf(newStatus.toUpperCase());
            personalInfo.setStatus(kycStatus);  // Update status field
            log.debug("Setting status to: {} for account: {}", kycStatus, maskAccountId(accountId));
            
            // Handle rejection reason based on status
            if (!handleRejectionReason(accountId, kycStatus, rejectionReason, personalInfo)) {
                return "Failed: Rejection reason is required when status is REJECTED";
            }
            
            inforepository.save(personalInfo); // Save the updated record
            log.info("Successfully updated KYC status to {} for account: {}", 
                    kycStatus, maskAccountId(accountId));
            
            return "KYC status for " + maskAccountId(accountId) + " updated to " + newStatus;
        } catch (IllegalArgumentException e) {
            log.error("Invalid KYC status value: {} for account: {}", 
                    newStatus, maskAccountId(accountId));
            return "Failed: Invalid KYC status value '" + newStatus + "'";
        }
    }
    
    /**
     * Handles the rejection reason logic based on status
     * @return true if handling was successful, false if there was a validation error
     */
    private boolean handleRejectionReason(String accountId, PersonalInfoStatus kycStatus, 
                                         String rejectionReason, PersonalInfoEntity personalInfo) {
        if (kycStatus == PersonalInfoStatus.REJECTED) {
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                log.warn("Missing rejection reason for REJECTED status for account: {}", 
                        maskAccountId(accountId));
                return false;
            }
            personalInfo.setRejectionReason(rejectionReason);
            log.debug("Setting rejection reason for account: {}", maskAccountId(accountId));
        } else {
            // Clear rejection fields if status is not REJECTED
            personalInfo.setRejectionReason(null);
            log.debug("Clearing rejection reason for account: {}", maskAccountId(accountId));
        }
        return true;
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
}
