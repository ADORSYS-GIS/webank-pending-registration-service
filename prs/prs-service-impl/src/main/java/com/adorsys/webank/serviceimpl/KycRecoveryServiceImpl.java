package com.adorsys.webank.serviceimpl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycRecoveryServiceApi;
import com.adorsys.error.ResourceNotFoundException;
import com.adorsys.error.ValidationException;

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
        Optional<PersonalInfoProjection> personalInfoOpt = inforepository.findByAccountId(accountId);

        if (personalInfoOpt.isEmpty()) {
            log.warn("No record found for accountId {}", accountId);
            throw new ResourceNotFoundException("No record found for accountId " + accountId);
        }

        PersonalInfoProjection personalInfo = personalInfoOpt.get();

        // Validate document details
        if (!personalInfo.getDocumentUniqueId().equals(idNumber)) {
            log.error("Document ID mismatch for accountId {}: expected {}, got {}", accountId, personalInfo.getDocumentUniqueId(), idNumber);
            throw new ValidationException("Document ID mismatch");
        }

        if (!personalInfo.getExpirationDate().equals(expiryDate)) {
            log.error("Document expiry date mismatch for accountId {}: expected {}, got {}", accountId, personalInfo.getExpirationDate(), expiryDate);
            throw new ValidationException("Document expiry date mismatch");
        }

        // If all validations pass
        log.info("Document verification successful for accountId {}", accountId);
        return "Document verification successful";
    }
}