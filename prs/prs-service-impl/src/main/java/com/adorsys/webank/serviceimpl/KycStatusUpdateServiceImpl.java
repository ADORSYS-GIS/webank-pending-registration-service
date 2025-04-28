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
    public String updateKycStatus(String accountId, String newStatus, String idNumber, String expiryDate) {
        log.info("Updating KYC status for accountId {} to {}", accountId, newStatus);

        Optional<PersonalInfoEntity> personalInfoOpt = inforepository.findByAccountId(accountId);
        if (personalInfoOpt.isPresent()) {
            PersonalInfoEntity personalInfo = personalInfoOpt.get();

            // Validate document details
            if (!personalInfo.getDocumentUniqueId().equals(idNumber)) {
                log.error("Document ID mismatch for accountId {}: expected {}, got {}", accountId, personalInfo.getDocumentUniqueId(), idNumber);
                return "Failed: Document ID mismatch";
            }

            if (!personalInfo.getExpirationDate().equals(expiryDate)) {
                log.error("Document expiry date mismatch for accountId {}: expected {}, got {}", accountId, personalInfo.getExpirationDate(), expiryDate);
                return "Failed: Document expiry date mismatch";
            }

            try {
                // Convert newStatus string to Enum
                PersonalInfoStatus kycStatus = PersonalInfoStatus.valueOf(newStatus.toUpperCase());
                personalInfo.setStatus(kycStatus);  // Update status field
                inforepository.save(personalInfo); // Save the updated record

                log.info("Successfully updated KYC status for accountId {}", accountId);
                return "KYC status for " + accountId + " updated to " + newStatus;
            } catch (IllegalArgumentException e) {
                log.error("Invalid KYC status value: {}", newStatus);
                return "Failed: Invalid KYC status value '" + newStatus + "'";
            }
        } else {
            log.warn("No record found for accountId {}", accountId);
            return "Failed: No record found for accountId " + accountId;
        }
    }
}
