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
        Optional<PersonalInfoEntity> personalInfoOpt = inforepository.findByAccountId(accountId);

        if (personalInfoOpt.isEmpty()) {
            log.warn("No record found for accountId {}", accountId);
            return "Failed: No record found for accountId " + accountId;
        }

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

        // If all validations pass
        log.info("Document verification successful for accountId {}", accountId);
        return "Document verification successful";
    }
}