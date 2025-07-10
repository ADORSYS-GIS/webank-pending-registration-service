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
import org.springframework.web.server.ResponseStatusException;

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
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                    });

            validateDocumentDetails(personalInfo, idNumber, expiryDate, correlationId);
            return updateStatus(personalInfo, newStatus, rejectionReason, correlationId);
            
        } catch (ResponseStatusException e) {
            return KycStatusUpdateResponse.builder()
                    .success(false)
                    .message(e.getReason())
                    .status(newStatus)
                    .accountId(accountId)
                    .build();
        } catch (Exception e) {
            log.error("Error updating KYC status for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId, e);
            return KycStatusUpdateResponse.builder()
                    .success(false)
                    .message("Failed to update KYC status: " + e.getMessage())
                    .status(newStatus)
                    .accountId(accountId)
                    .build();
        }
    }

    private void validateDocumentDetails(PersonalInfoEntity personalInfo, String idNumber, String expiryDate, String correlationId) {
        if (idNumber == null || !idNumber.equals(personalInfo.getDocumentUniqueId())) {
            String errorMsg = "Document ID mismatch for account: " + maskAccountId(personalInfo.getAccountId());
            log.warn("{} [correlationId={}]", errorMsg, correlationId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
        }

        if (expiryDate == null || !expiryDate.equals(personalInfo.getExpirationDate())) {
            String errorMsg = "Document expiry date mismatch for account: " + maskAccountId(personalInfo.getAccountId());
            log.warn("{} [correlationId={}]", errorMsg, correlationId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
        }
    }

    private KycStatusUpdateResponse updateStatus(PersonalInfoEntity personalInfo, String newStatus, String rejectionReason, String correlationId) {
        try {
            PersonalInfoStatus status = PersonalInfoStatus.valueOf(newStatus);
            personalInfo.setStatus(status);
            
            if (status == PersonalInfoStatus.REJECTED && (rejectionReason == null || rejectionReason.trim().isEmpty())) {
                String errorMsg = "Rejection reason is required for REJECTED status";
                log.warn("{} [correlationId={}]", errorMsg, correlationId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
            }
            
            personalInfo.setRejectionReason(rejectionReason);
            PersonalInfoEntity updatedEntity = inforepository.save(personalInfo);
            
            log.info("Successfully updated KYC status to {} for account: {} [correlationId={}]",
                    newStatus, maskAccountId(personalInfo.getAccountId()), correlationId);
                    
            return KycStatusUpdateResponse.builder()
                    .success(true)
                    .message("KYC status updated successfully to " + newStatus)
                    .status(newStatus)
                    .accountId(updatedEntity.getAccountId())
                    .build();
            
        } catch (IllegalArgumentException e) {
            String errorMsg = "Invalid status value: " + newStatus;
            log.warn("{} [correlationId={}]", errorMsg, correlationId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg, e);
        }
    }

    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return "****";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
}