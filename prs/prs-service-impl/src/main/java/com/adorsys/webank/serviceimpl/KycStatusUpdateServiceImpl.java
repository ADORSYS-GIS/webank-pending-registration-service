package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.dto.response.KycStatusUpdateResponse;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycStatusUpdateServiceImpl implements KycStatusUpdateServiceApi {

    private final PersonalInfoRepository inforepository;

    @Override
    @Transactional
    public KycStatusUpdateResponse updateKycStatus(String accountId, String newStatus, String idNumber, String expiryDate, String rejectionReason) {
        String correlationId = MDC.get("correlationId");
        log.info("Updating KYC status for account: {} to status: {} [correlationId={}]",
                maskAccountId(accountId), newStatus, correlationId);

        try {
            PersonalInfoEntity personalInfo = inforepository.findById(accountId)
                    .orElseThrow(() -> {
                        String errorMsg = "No KYC record found for accountId: " + maskAccountId(accountId);
                        log.warn("{} [correlationId={}]", errorMsg, correlationId);
                        return new EntityNotFoundException(errorMsg);
                    });

            KycStatusUpdateResponse validationError = validateDocumentDetails(personalInfo, idNumber, expiryDate, correlationId);
            if (validationError != null) {
                return validationError;
            }

            return updateStatus(personalInfo, newStatus, rejectionReason, correlationId);
        } catch (Exception e) {
            log.error("Error updating KYC status for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId, e);
            return KycStatusUpdateResponse.error(
                    "An error occurred while processing your request: " + e.getMessage(),
                    accountId,
                    idNumber
            );
        }
    }

    private KycStatusUpdateResponse validateDocumentDetails(PersonalInfoEntity personalInfo, String idNumber, String expiryDate, String correlationId) {
        String accountId = personalInfo.getAccountId();
        
        if (idNumber == null || !idNumber.equals(personalInfo.getDocumentUniqueId())) {
            String errorMsg = "Document ID mismatch for account: " + maskAccountId(accountId);
            log.warn("{} [correlationId={}]", errorMsg, correlationId);
            return KycStatusUpdateResponse.error(
                    "Document verification failed: ID number does not match records",
                    accountId,
                    idNumber
            );
        }

        if (expiryDate == null || !expiryDate.equals(personalInfo.getExpirationDate())) {
            String errorMsg = "Document expiry date mismatch for account: " + maskAccountId(accountId);
            log.warn("{} [correlationId={}]", errorMsg, correlationId);
            return KycStatusUpdateResponse.error(
                    "Document verification failed: Expiry date does not match records",
                    accountId,
                    idNumber
            );
        }
        return null;
    }

    private KycStatusUpdateResponse updateStatus(PersonalInfoEntity personalInfo, String newStatus, String rejectionReason, String correlationId) {
        String accountId = personalInfo.getAccountId();
        String documentId = personalInfo.getDocumentUniqueId();
        
        try {
            PersonalInfoStatus kycStatus = PersonalInfoStatus.valueOf(newStatus.toUpperCase());

            if (kycStatus == PersonalInfoStatus.REJECTED) {
                if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                    String errorMsg = "Missing rejection reason for REJECTED status for account: " + maskAccountId(accountId);
                    log.warn("{} [correlationId={}]", errorMsg, correlationId);
                    return KycStatusUpdateResponse.error(
                            "Rejection reason is required when status is REJECTED",
                            accountId,
                            documentId
                    );
                }
                personalInfo.setRejectionReason(rejectionReason);
            } else {
                personalInfo.setRejectionReason(null); // Clear rejection reason if status is not REJECTED
            }

            personalInfo.setStatus(kycStatus);
            inforepository.save(personalInfo);
            
            String successMsg = String.format("KYC status successfully updated to %s", newStatus);
            log.info("{} for account: {} [correlationId={}]", successMsg, maskAccountId(accountId), correlationId);

            return KycStatusUpdateResponse.success(
                    successMsg,
                    newStatus,
                    accountId,
                    documentId
            );

        } catch (IllegalArgumentException e) {
            String errorMsg = String.format("Invalid KYC status provided: %s", newStatus);
            log.error("{} for account: {} [correlationId={}]", errorMsg, maskAccountId(accountId), correlationId, e);
            return KycStatusUpdateResponse.error(
                    errorMsg,
                    accountId,
                    documentId
            );
        }
    }

    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return "****";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
}