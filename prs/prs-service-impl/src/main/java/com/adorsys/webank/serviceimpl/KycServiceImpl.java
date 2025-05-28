package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.dto.response.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import jakarta.persistence.*;
import org.slf4j.*;
import org.springframework.stereotype.*;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class KycServiceImpl implements KycServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycServiceImpl.class);
    private final UserDocumentsRepository repository;
    private final PersonalInfoRepository inforepository;

    public KycServiceImpl(PersonalInfoRepository inforepository, UserDocumentsRepository repository) {
        this.inforepository = inforepository;
        this.repository = repository;
    }

    @Override
    public KycDocumentResponse sendKycDocument(String AccountId, KycDocumentRequest kycDocumentRequest) {
        if (kycDocumentRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Document Request");
        }

        try {
            log.info("Processing KYC Document for accountId: {}", AccountId);

            // Check if document already exists
            Optional<UserDocumentsEntity> existingDocOpt = repository.findByAccountId(AccountId);
            UserDocumentsEntity userDocuments;

            if (existingDocOpt.isPresent()) {
                // Update existing document
                userDocuments = existingDocOpt.get();
                userDocuments.setFrontID(kycDocumentRequest.getFrontId());
                userDocuments.setBackID(kycDocumentRequest.getBackId());
                userDocuments.setSelfieID(kycDocumentRequest.getSelfieId());
                userDocuments.setTaxID(kycDocumentRequest.getTaxId());
                userDocuments.setStatus(UserDocumentsStatus.PENDING);
                log.debug("Updating existing UserDocumentsEntity: {}", userDocuments);
            } else {
                // Create new document
                userDocuments = UserDocumentsEntity.builder()
                        .accountId(AccountId)
                        .frontID(kycDocumentRequest.getFrontId())
                        .backID(kycDocumentRequest.getBackId())
                        .selfieID(kycDocumentRequest.getSelfieId())
                        .taxID(kycDocumentRequest.getTaxId())
                        .status(UserDocumentsStatus.PENDING)
                        .build();
                log.debug("Creating new UserDocumentsEntity: {}", userDocuments);
            }

            // Save to DB
            repository.save(userDocuments);
            log.info("KYC Document saved successfully for accountId: {}", AccountId);
            
            KycDocumentResponse response = new KycDocumentResponse();
            response.setKycId("kyc_doc_" + System.currentTimeMillis());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(LocalDateTime.now());
            response.setMessage("KYC Document sent successfully and saved");
            response.setAccountId(AccountId);
            return response;
        } catch (Exception e) {
            log.error("Failed to send KYC Document for accountId: {}", AccountId, e);
            throw new KycProcessingException("Failed to send KYC Document: " + e.getMessage());
        }
    }

    @Override
    public KycInfoResponse sendKycInfo(String AccountId, KycInfoRequest kycInfoRequest) {
        if (kycInfoRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Info Request");
        }

        try {
            log.info("Processing KYC Info for accountId: {}", AccountId);

            Optional<PersonalInfoEntity> existingInfoOpt = inforepository.findByAccountId(AccountId);
            PersonalInfoEntity personalInfoEntity;

            if (existingInfoOpt.isPresent()) {
                personalInfoEntity = existingInfoOpt.get();
                personalInfoEntity.setDocumentUniqueId(kycInfoRequest.getIdNumber());
                personalInfoEntity.setExpirationDate(kycInfoRequest.getExpiryDate());
                personalInfoEntity.setStatus(PersonalInfoStatus.PENDING);
                log.debug("Updating existing PersonalInfoEntity: {}", personalInfoEntity);
            } else {
                personalInfoEntity = PersonalInfoEntity.builder()
                        .accountId(AccountId)
                        .documentUniqueId(kycInfoRequest.getIdNumber())
                        .expirationDate(kycInfoRequest.getExpiryDate())
                        .status(PersonalInfoStatus.PENDING)
                        .build();
                log.debug("Creating new PersonalInfoEntity: {}", personalInfoEntity);
            }

            inforepository.save(personalInfoEntity);
            log.info("KYC Info saved successfully for accountId: {}", AccountId);
            
            KycInfoResponse response = new KycInfoResponse();
            response.setKycId("kyc_info_" + System.currentTimeMillis());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(LocalDateTime.now());
            response.setMessage("KYC Info sent successfully and saved.");
            response.setAccountId(AccountId);
            response.setIdNumber(kycInfoRequest.getIdNumber());
            response.setExpiryDate(kycInfoRequest.getExpiryDate());
            response.setVerificationStatus(KycInfoResponse.VerificationStatus.PENDING);
            return response;
        } catch (Exception e) {
            log.error("Failed to send KYC Info for accountId: {}", AccountId, e);
            throw new KycProcessingException("Failed to send KYC Info: " + e.getMessage());
        }
    }

    @Override
    public KycLocationResponse sendKycLocation(KycLocationRequest kycLocationRequest) {
        if (kycLocationRequest == null || kycLocationRequest.getLocation() == null) {
            throw new IllegalArgumentException("Invalid KYC Location Request");
        }

        try {
            log.info("Processing KYC Location update for accountId: {}", kycLocationRequest.getAccountId());

            String accountId = kycLocationRequest.getAccountId();
            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoAccountId(accountId);

            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = existingInfo.get();
                personalInfo.setLocation(kycLocationRequest.getLocation());
                inforepository.save(personalInfo);
                log.info("KYC Location updated successfully for accountId: {}", accountId);
                
                KycLocationResponse response = new KycLocationResponse();
                response.setKycId("kyc_location_" + System.currentTimeMillis());
                response.setStatus(KycResponse.KycStatus.PENDING);
                response.setSubmittedAt(LocalDateTime.now());
                response.setMessage("KYC Location updated successfully.");
                response.setAccountId(accountId);
                response.setLocation(kycLocationRequest.getLocation());
                response.setVerificationStatus(KycLocationResponse.VerificationStatus.PENDING);
                return response;
            } else {
                throw new EntityNotFoundException("No KYC record found for the provided accountId.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Location for accountId: {}", kycLocationRequest.getAccountId(), e);
            throw new KycProcessingException("Failed to update KYC Location: " + e.getMessage());
        }
    }

    @Override
    public KycEmailResponse sendKycEmail(KycEmailRequest kycEmailRequest) {
        if (kycEmailRequest == null || kycEmailRequest.getEmail() == null) {
            throw new IllegalArgumentException("Invalid KYC Email Request");
        }

        try {
            log.info("Processing KYC Email update for accountId: {}", kycEmailRequest.getAccountId());

            String accountId = kycEmailRequest.getAccountId();
            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoAccountId(accountId);

            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = existingInfo.get();
                personalInfo.setEmail(kycEmailRequest.getEmail());
                inforepository.save(personalInfo);
                log.info("KYC Email updated successfully for accountId: {}", accountId);
                
                KycEmailResponse response = new KycEmailResponse();
                response.setKycId("kyc_email_" + System.currentTimeMillis());
                response.setStatus(KycResponse.KycStatus.PENDING);
                response.setSubmittedAt(LocalDateTime.now());
                response.setMessage("KYC Email updated successfully.");
                response.setAccountId(accountId);
                response.setEmail(kycEmailRequest.getEmail());
                response.setVerificationStatus(KycEmailResponse.VerificationStatus.VERIFICATION_SENT);
                return response;
            } else {
                throw new EntityNotFoundException("No KYC record found for the provided accountId.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Email for accountId: {}", kycEmailRequest.getAccountId(), e);
            throw new KycProcessingException("Failed to update KYC Email: " + e.getMessage());
        }
    }

    @Override
    public Optional<PersonalInfoEntity> getPersonalInfoAccountId(String accountId) {
        return inforepository.findByAccountId(accountId);
    }

    @Override
    public List<UserInfoResponse> getPendingKycRecords() {
        // Get all personal info records with pending status
        List<PersonalInfoEntity> pendingPersonalInfos = inforepository.findByStatus(PersonalInfoStatus.PENDING);
        List<UserInfoResponse> responses = new ArrayList<>();

        for (PersonalInfoEntity info : pendingPersonalInfos) {
            // For each pending personal info, check if there's a matching document
            Optional<UserDocumentsEntity> documentsOpt = repository.findByAccountId(info.getAccountId());

            // Only add to response if user document exists and is pending
            if (documentsOpt.isPresent() && documentsOpt.get().getStatus() == UserDocumentsStatus.PENDING) {
                responses.add(mapToUserInfoResponse(info, documentsOpt));
            }
        }

        return responses;
    }

    @Override
    public List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId) {
        log.info("Finding KYC records for documentUniqueId: {}", documentUniqueId);

        List<PersonalInfoEntity> personalInfoList = inforepository.findByDocumentUniqueId(documentUniqueId);
        List<UserInfoResponse> responseList = new ArrayList<>();

        for (PersonalInfoEntity personalInfo : personalInfoList) {
            try {
                log.info("Processing PersonalInfoEntity: {}", personalInfo);
                Optional<UserDocumentsEntity> documentsOpt = repository.findByAccountId(personalInfo.getAccountId());
                responseList.add(mapToUserInfoResponse(personalInfo, documentsOpt));
            } catch (Exception e) {
                log.error("Error processing PersonalInfoEntity: {}", personalInfo, e);
            }
        }

        log.info("Final UserInfoResponse List: {}", responseList);
        return responseList;
    }

    // Helper method to map PersonalInfoEntity to UserInfoResponse
    private UserInfoResponse mapToUserInfoResponse(PersonalInfoEntity personalInfo, Optional<UserDocumentsEntity> documentsOpt) {
        UserInfoResponse response = new UserInfoResponse();
        response.setAccountId(personalInfo.getAccountId());
        response.setEmail(personalInfo.getEmail());
        response.setIdNumber(personalInfo.getDocumentUniqueId());
        response.setExpirationDate(personalInfo.getExpirationDate());
        response.setLocation(personalInfo.getLocation());
        response.setStatus(personalInfo.getStatus().toString());
        response.setRejectionReason(personalInfo.getRejectionReason());

        // Add document details if available
        documentsOpt.ifPresent(doc -> {
            response.setFrontID(doc.getFrontID());
            response.setBackID(doc.getBackID());
            response.setSelfie(doc.getSelfieID());
            response.setTaxDocument(doc.getTaxID());
        });

        return response;
    }
}