package com.adorsys.webank.serviceimpl;

import com.adorsys.error.AccountNotFoundException;
import com.adorsys.error.KycProcessingException;
import com.adorsys.error.ValidationException;
import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.domain.UserDocumentsStatus;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.dto.response.*;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.projection.UserDocumentsProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.repository.UserDocumentsRepository;
import com.adorsys.webank.service.KycServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycServiceImpl implements KycServiceApi {

    private final UserDocumentsRepository repository;
    private final PersonalInfoRepository inforepository;

    @Override
    public KycDocumentResponse sendKycDocument(String accountId, KycDocumentRequest kycDocumentRequest) {
        String correlationId = MDC.get("correlationId");
        if (kycDocumentRequest == null) {
            log.warn("Invalid KYC Document Request received for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            throw new ValidationException("Invalid KYC Document Request");
        }

        try {
            log.info("Processing KYC Document for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);

            // Check if document already exists
            Optional<UserDocumentsProjection> existingDocOpt = repository.findByAccountId(accountId);
            UserDocumentsEntity userDocuments;

            if (existingDocOpt.isPresent()) {
                // Update existing document
                userDocuments = new UserDocumentsEntity();
                userDocuments.setAccountId(accountId);
                userDocuments.setFrontID(kycDocumentRequest.getFrontId());
                userDocuments.setBackID(kycDocumentRequest.getBackId());
                userDocuments.setSelfieID(kycDocumentRequest.getSelfieId());
                userDocuments.setTaxID(kycDocumentRequest.getTaxId());
                userDocuments.setStatus(UserDocumentsStatus.PENDING);
                log.debug("Updating existing document for accountId: {} [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
            } else {
                // Create new document
                userDocuments = UserDocumentsEntity.builder()
                        .accountId(accountId)
                        .frontID(kycDocumentRequest.getFrontId())
                        .backID(kycDocumentRequest.getBackId())
                        .selfieID(kycDocumentRequest.getSelfieId())
                        .taxID(kycDocumentRequest.getTaxId())
                        .status(UserDocumentsStatus.PENDING)
                        .build();
                log.debug("Creating new document for accountId: {} [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
            }

            // Save to DB
            repository.save(userDocuments);
            log.info("KYC Document saved successfully for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            // Build response DTO
            KycDocumentResponse response = new KycDocumentResponse();
            response.setKycId(userDocuments.getAccountId());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(java.time.LocalDateTime.now());
            response.setMessage("KYC Document sent successfully and saved");
            // Populate other fields as needed
            return response;
        } catch (Exception e) {
            log.error("Failed to send KYC Document for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId, e);
            throw new KycProcessingException("Failed to send KYC Document: " + e.getMessage());
        }
    }

    @Override
    public KycInfoResponse sendKycInfo(String accountId, KycInfoRequest kycInfoRequest) {
        String correlationId = MDC.get("correlationId");
        if (kycInfoRequest == null) {
            log.warn("Invalid KYC Info Request received for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            throw new ValidationException("Invalid KYC Info Request");
        }

        try {
            log.info("Processing KYC Info for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            
            // Log masked sensitive information
            if (log.isDebugEnabled()) {
                log.debug("Processing ID number: {}, expiry date: {} [correlationId={}]", 
                    maskIdNumber(kycInfoRequest.getIdNumber()), 
                    kycInfoRequest.getExpiryDate(),
                    correlationId);
            }

            Optional<PersonalInfoProjection> existingInfoOpt = inforepository.findByAccountId(accountId);
            PersonalInfoEntity personalInfoEntity;

            if (existingInfoOpt.isPresent()) {
                personalInfoEntity = new PersonalInfoEntity();
                personalInfoEntity.setAccountId(accountId);
                personalInfoEntity.setDocumentUniqueId(kycInfoRequest.getIdNumber());
                personalInfoEntity.setExpirationDate(kycInfoRequest.getExpiryDate());
                personalInfoEntity.setStatus(PersonalInfoStatus.PENDING);
                log.debug("Updating existing personal info for accountId: {} [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
            } else {
                personalInfoEntity = PersonalInfoEntity.builder()
                        .accountId(accountId)
                        .documentUniqueId(kycInfoRequest.getIdNumber())
                        .expirationDate(kycInfoRequest.getExpiryDate())
                        .status(PersonalInfoStatus.PENDING)
                        .build();
                log.debug("Creating new personal info for accountId: {} [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
            }

            inforepository.save(personalInfoEntity);
            log.info("KYC Info saved successfully for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            // Build response DTO
            KycInfoResponse response = new KycInfoResponse();
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(java.time.LocalDateTime.now());
            response.setMessage("KYC Info submitted successfully");
            // Populate other fields as needed
            return response;

        } catch (Exception e) {
            log.error("Failed to send KYC Info for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId, e);
            throw new KycProcessingException("Failed to send KYC Info: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public KycLocationResponse sendKycLocation(KycLocationRequest kycLocationRequest) {
        String correlationId = MDC.get("correlationId");
        if (kycLocationRequest == null || kycLocationRequest.getLocation() == null) {
            log.warn("Invalid KYC Location Request received [correlationId={}]", correlationId);
            throw new ValidationException("Invalid KYC Location Request");
        }

        String accountId = kycLocationRequest.getAccountId();

        PersonalInfoEntity personalInfo = inforepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("No KYC record found for accountId: {} [correlationId={}]", 
                            maskAccountId(accountId), correlationId);
                    return new AccountNotFoundException("No KYC record found for the provided accountId.");
                });

        try {
            log.info("Processing KYC Location update for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            log.debug("Location: {} [correlationId={}]", 
                    kycLocationRequest.getLocation(), correlationId);

            personalInfo.setLocation(kycLocationRequest.getLocation());
            inforepository.save(personalInfo);
            log.info("KYC Location updated successfully for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            // Build response DTO
            KycLocationResponse response = new KycLocationResponse();
            response.setKycId(kycLocationRequest.getAccountId());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(java.time.LocalDateTime.now());
            response.setMessage("KYC Location submitted successfully");
            // Populate other fields as needed
            return response;

        } catch (Exception e) {
            log.error("Failed to update KYC Location for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId, e);
            throw new KycProcessingException("Failed to update KYC Location: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public KycEmailResponse sendKycEmail(KycEmailRequest kycEmailRequest) {
        String correlationId = MDC.get("correlationId");
        if (kycEmailRequest == null || kycEmailRequest.getEmail() == null || kycEmailRequest.getEmail().isEmpty()) {
            log.warn("Invalid KYC Email Request received [correlationId={}]", correlationId);
            throw new ValidationException("Invalid KYC Email Request");
        }

        String accountId = kycEmailRequest.getAccountId();

        PersonalInfoEntity personalInfo = inforepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("No KYC record found for accountId: {} [correlationId={}]", 
                            maskAccountId(accountId), correlationId);
                    return new AccountNotFoundException("No KYC record found for the provided accountId.");
                });
        try {
            log.info("Processing KYC Email update for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            log.debug("Email: {} [correlationId={}]", 
                    kycEmailRequest.getEmail(), correlationId);

            personalInfo.setEmail(kycEmailRequest.getEmail());
            inforepository.save(personalInfo);
            log.info("KYC Email updated successfully for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            // Build response DTO
            KycEmailResponse response = new KycEmailResponse();
            response.setKycId(kycEmailRequest.getAccountId());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(java.time.LocalDateTime.now());
            response.setMessage("KYC Email submitted successfully");
            // Populate other fields as needed
            return response;

        } catch (Exception e) {
            log.error("Failed to update KYC Email for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId, e);
            throw new KycProcessingException("Failed to update KYC Email: " + e.getMessage());
        }
    }

    @Override
    public Optional<PersonalInfoProjection> getPersonalInfoAccountId(String accountId) {
        String correlationId = MDC.get("correlationId");
        log.debug("Retrieving personal info for accountId: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        return inforepository.findByAccountId(accountId);
    }

    @Override
    public List<UserInfoResponse> getPendingKycRecords() {
        String correlationId = MDC.get("correlationId");
        log.info("Retrieving all pending KYC records [correlationId={}]", correlationId);
        
        // Get all personal info records with pending status
        List<PersonalInfoProjection> pendingPersonalInfos = inforepository.findByStatus(PersonalInfoStatus.PENDING);
        log.debug("Found {} pending personal info records [correlationId={}]", 
                pendingPersonalInfos.size(), correlationId);
        
        List<UserInfoResponse> responses = new ArrayList<>();

        for (PersonalInfoProjection info : pendingPersonalInfos) {
            // For each pending personal info, check if there's a matching document
            Optional<UserDocumentsProjection> documentsOpt = repository.findByAccountId(info.getAccountId());

            // Only add to response if user document exists and is pending
            if (documentsOpt.isPresent() && documentsOpt.get().getStatus() == UserDocumentsStatus.PENDING) {
                responses.add(mapToUserInfoResponse(info, documentsOpt));
                log.debug("Added record for accountId: {} to response [correlationId={}]", 
                        maskAccountId(info.getAccountId()), correlationId);
            }
        }

        log.info("Returning {} pending KYC records [correlationId={}]", 
                responses.size(), correlationId);
        return responses;
    }

    @Override
    public List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId) {
        String correlationId = MDC.get("correlationId");
        log.info("Finding KYC records for documentUniqueId: {} [correlationId={}]", 
                maskIdNumber(documentUniqueId), correlationId);

        List<PersonalInfoProjection> personalInfoList = inforepository.findByDocumentUniqueId(documentUniqueId);
        log.debug("Found {} personal info records with the given document ID [correlationId={}]", 
                personalInfoList.size(), correlationId);
        
        List<UserInfoResponse> responseList = new ArrayList<>();

        for (PersonalInfoProjection personalInfo : personalInfoList) {
            try {
                log.debug("Processing record for accountId: {} [correlationId={}]", 
                        maskAccountId(personalInfo.getAccountId()), correlationId);
                Optional<UserDocumentsProjection> documentsOpt = repository.findByAccountId(personalInfo.getAccountId());
                responseList.add(mapToUserInfoResponse(personalInfo, documentsOpt));
            } catch (Exception e) {
                log.error("Error processing record for accountId: {} [correlationId={}]", 
                    maskAccountId(personalInfo.getAccountId()), correlationId, e);
            }
        }

        log.info("Returning {} KYC records [correlationId={}]", 
                responseList.size(), correlationId);
        return responseList;
    }

    private UserInfoResponse mapToUserInfoResponse(PersonalInfoProjection info, Optional<UserDocumentsProjection> documentsOpt) {
        UserInfoResponse response = new UserInfoResponse();
        response.setAccountId(info.getAccountId());
        response.setIdNumber(info.getDocumentUniqueId());
        response.setExpirationDate(info.getExpirationDate());
        response.setLocation(info.getLocation());
        response.setEmail(info.getEmail());
        response.setStatus(info.getStatus().name());

        if (documentsOpt.isPresent()) {
            UserDocumentsProjection documents = documentsOpt.get();
            response.setFrontID(documents.getFrontID());
            response.setBackID(documents.getBackID());
            response.setSelfie(documents.getSelfieID());
            response.setTaxDocument(documents.getTaxID());
        }

        response.setRejectionReason(info.getRejectionReason());
        return response;
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
        if (idNumber == null || idNumber.length() <= 4) {
            return "********";
        }
        return idNumber.substring(0, 2) + "********" + idNumber.substring(idNumber.length() - 2);
    }
}