package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.adorsys.webank.projection.*;
import org.slf4j.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import org.slf4j.MDC;

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
        String correlationId = MDC.get("correlationId");
        log.info("Updating KYC status for account: {} to status: {} [correlationId={}]", 
                maskAccountId(accountId), newStatus, correlationId);
        
        if (rejectionReason != null && !rejectionReason.isEmpty()) {
            log.debug("Rejection reason provided [correlationId={}]", correlationId);
        }

        Optional<PersonalInfoProjection> personalInfoOpt = inforepository.findByAccountId(accountId);
        if (personalInfoOpt.isPresent()) {
            PersonalInfoProjection personalInfo = personalInfoOpt.get();
            
            log.debug("Found personal info record for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);

            // Validate document ID
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

            try {
                // Convert newStatus string to Enum
                PersonalInfoStatus kycStatus = PersonalInfoStatus.valueOf(newStatus.toUpperCase());
                
                // Create new entity with updated status
                PersonalInfoEntity updatedInfo = new PersonalInfoEntity();
                updatedInfo.setAccountId(accountId);
                updatedInfo.setDocumentUniqueId(personalInfo.getDocumentUniqueId());
                updatedInfo.setExpirationDate(personalInfo.getExpirationDate());
                updatedInfo.setStatus(kycStatus);
                
                log.debug("Setting status to: {} for account: {} [correlationId={}]", 
                        kycStatus, maskAccountId(accountId), correlationId);
                
                // Set rejection fields if status is REJECTED
                if (kycStatus == PersonalInfoStatus.REJECTED) {
                    if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                        log.warn("Missing rejection reason for REJECTED status for account: {} [correlationId={}]", 
                                maskAccountId(accountId), correlationId);
                        return "Failed: Rejection reason is required when status is REJECTED";
                    }
                    updatedInfo.setRejectionReason(rejectionReason);
                    log.debug("Setting rejection reason for account: {} [correlationId={}]", 
                            maskAccountId(accountId), correlationId);
                } else {
                    // Clear rejection fields if status is not REJECTED
                    updatedInfo.setRejectionReason(null);
                    log.debug("Clearing rejection reason for account: {} [correlationId={}]", 
                            maskAccountId(accountId), correlationId);
                }
                
                inforepository.save(updatedInfo); // Save the updated record

                log.info("Successfully updated KYC status for account: {} [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
                return "KYC status for " + accountId + " updated to " + newStatus;
            } catch (IllegalArgumentException e) {
                log.error("Invalid KYC status value: {} for account: {} [correlationId={}]", 
                        newStatus, maskAccountId(accountId), correlationId, e);
                return "Failed: Invalid KYC status value '" + newStatus + "'";
            }
        } else {
            log.warn("No record found for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            return "Failed: No record found for accountId " + accountId;
        }
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
