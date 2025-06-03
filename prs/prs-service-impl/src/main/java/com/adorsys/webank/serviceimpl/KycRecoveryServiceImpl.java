package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import org.slf4j.*;
import org.springframework.transaction.annotation.*;
import org.springframework.stereotype.Service;


import java.util.*;
@Service
public class KycRecoveryServiceImpl implements KycRecoveryServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycRecoveryServiceImpl.class);
    private final PersonalInfoRepository inforepository;

    public KycRecoveryServiceImpl(PersonalInfoRepository inforepository) {
        this.inforepository = inforepository;
    }

    @Override
    @Transactional
    public String verifyKycRecoveryFields(String accountId, String idNumber, String expiryDate) {
        log.info("Verifying KYC recovery fields for account: {}", maskAccountId(accountId));
        log.debug("Verifying with ID: {}, expiry date: {}", maskIdNumber(idNumber), expiryDate);
        
        Optional<PersonalInfoEntity> personalInfoOpt = inforepository.findByAccountId(accountId);

        if (personalInfoOpt.isEmpty()) {
            log.warn("No record found for account: {}", maskAccountId(accountId));
            return "Failed: No record found for accountId " + accountId;
        }

        PersonalInfoEntity personalInfo = personalInfoOpt.get();
        log.debug("Found personal info record for account: {}", maskAccountId(accountId));

        // Validate document details
        if (!personalInfo.getDocumentUniqueId().equals(idNumber)) {
            log.warn("Document ID mismatch for account: {}", maskAccountId(accountId));
            return "Failed: Document ID mismatch";
        }

        if (!personalInfo.getExpirationDate().equals(expiryDate)) {
            log.warn("Document expiry date mismatch for account: {}", maskAccountId(accountId));
            return "Failed: Document expiry date mismatch";
        }

        // If all validations pass
        log.info("Document verification successful for account: {}", maskAccountId(accountId));
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