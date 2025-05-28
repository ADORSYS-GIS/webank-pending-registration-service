package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import org.slf4j.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import com.adorsys.error.ResourceNotFoundException;
import com.adorsys.error.ValidationException;

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
        if (accountId == null || accountId.isEmpty()) {
            throw new ValidationException("Account ID is required");
        }
        if (newStatus == null || newStatus.isEmpty()) {
            throw new ValidationException("Status is required");
        }
        if (idNumber == null || idNumber.isEmpty()) {
            throw new ValidationException("ID number is required");
        }
        if (expiryDate == null || expiryDate.isEmpty()) {
            throw new ValidationException("Expiry date is required");
        }

        log.info("Updating KYC status for accountId {} to {}", accountId, newStatus);

        Optional<PersonalInfoEntity> personalInfoOpt = inforepository.findByAccountId(accountId);
        if (personalInfoOpt.isPresent()) {
            PersonalInfoEntity personalInfo = personalInfoOpt.get();

            // Validate document details
            if (!personalInfo.getDocumentUniqueId().equals(idNumber)) {
                log.error("Document ID mismatch for accountId {}: expected {}, got {}", accountId, personalInfo.getDocumentUniqueId(), idNumber);
                throw new ValidationException("Document ID mismatch");
            }

            if (!personalInfo.getExpirationDate().equals(expiryDate)) {
                log.error("Document expiry date mismatch for accountId {}: expected {}, got {}", accountId, personalInfo.getExpirationDate(), expiryDate);
                throw new ValidationException("Document expiry date mismatch");
            }

            try {
                // Convert newStatus string to Enum
                PersonalInfoStatus kycStatus = PersonalInfoStatus.valueOf(newStatus.toUpperCase());
                personalInfo.setStatus(kycStatus);  // Update status field
                
                // Set rejection fields if status is REJECTED
                if (kycStatus == PersonalInfoStatus.REJECTED) {
                    if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                        throw new ValidationException("Rejection reason is required when status is REJECTED");
                    }
                    personalInfo.setRejectionReason(rejectionReason);
                } else {
                    // Clear rejection fields if status is not REJECTED
                    personalInfo.setRejectionReason(null);
                }
                
                inforepository.save(personalInfo); // Save the updated record

                log.info("Successfully updated KYC status for accountId {}", accountId);
                return "KYC status for " + accountId + " updated to " + newStatus;
            } catch (IllegalArgumentException e) {
                log.error("Invalid KYC status value: {}", newStatus);
                throw new ValidationException("Invalid KYC status value '" + newStatus + "'");
            }
        } else {
            log.warn("No record found for accountId {}", accountId);
            throw new ResourceNotFoundException("No record found for accountId " + accountId);
        }
    }
}
