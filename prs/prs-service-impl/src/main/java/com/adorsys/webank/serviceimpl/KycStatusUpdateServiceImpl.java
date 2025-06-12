package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        PersonalInfoEntity personalInfo = inforepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("No personal info record found for accountId: {} [correlationId={}]",
                            maskAccountId(accountId), correlationId);
                    return new EntityNotFoundException("No KYC record found for accountId: " + accountId);
                });

        // Validate document details
        if (idNumber == null || !idNumber.equals(personalInfo.getDocumentUniqueId())) {
            log.warn("Document ID mismatch for account: {} [correlationId={}]",
                    maskAccountId(accountId), correlationId);
            return "Failed: Document ID mismatch";
        }

        if (expiryDate == null || !expiryDate.equals(personalInfo.getExpirationDate())) {
            log.warn("Document expiry date mismatch for account: {} [correlationId={}]",
                    maskAccountId(accountId), correlationId);
            return "Failed: Document expiry date mismatch";
        }

        try {
            PersonalInfoStatus kycStatus = PersonalInfoStatus.valueOf(newStatus.toUpperCase());

            if (kycStatus == PersonalInfoStatus.REJECTED && (rejectionReason == null || rejectionReason.trim().isEmpty())) {
                log.warn("Missing rejection reason for REJECTED status for account: {} [correlationId={}]",
                        maskAccountId(accountId), correlationId);
                return "Failed: Rejection reason is required when status is REJECTED";
            }

            personalInfo.setStatus(kycStatus);
            if (kycStatus == PersonalInfoStatus.REJECTED) {
                personalInfo.setRejectionReason(rejectionReason);
            } else {
                personalInfo.setRejectionReason(null); // Clear rejection reason if status is not REJECTED
            }

            inforepository.save(personalInfo);
            log.info("Successfully updated KYC status for account: {} to {} [correlationId={}]",
                    maskAccountId(accountId), newStatus, correlationId);

            return "KYC status updated successfully to " + newStatus;

        } catch (IllegalArgumentException e) {
            log.error("Invalid KYC status value: {} for account: {} [correlationId={}]",
                    newStatus, maskAccountId(accountId), correlationId, e);
            return "Failed: Invalid KYC status value '" + newStatus + "'";
        }
    }

    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return "****";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
}
