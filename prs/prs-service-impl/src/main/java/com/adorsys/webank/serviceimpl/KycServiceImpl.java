package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.adorsys.webank.projection.*;
import jakarta.persistence.*;
import org.slf4j.*;
import org.springframework.stereotype.*;

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
    public String sendKycDocument(String AccountId, KycDocumentRequest kycDocumentRequest) {
        if (kycDocumentRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Document Request");
        }

        try {
            log.info("Processing KYC Document for accountId: {}", AccountId);

            // Check if document already exists
            Optional<UserDocumentsProjection> existingDocOpt = repository.findByAccountId(AccountId);
            UserDocumentsEntity userDocuments;

            if (existingDocOpt.isPresent()) {
                // Update existing document
                userDocuments = new UserDocumentsEntity();
                userDocuments.setAccountId(AccountId);
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
            return "KYC Document sent successfully and saved";
        } catch (Exception e) {
            log.error("Failed to send KYC Document for accountId: {}", AccountId, e);
            throw new KycProcessingException("Failed to send KYC Document: " + e.getMessage());
        }
    }

    @Override
    public String sendKycInfo(String AccountId, KycInfoRequest kycInfoRequest) {
        if (kycInfoRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Info Request");
        }

        try {
            log.info("Processing KYC Info for accountId: {}", AccountId);

            Optional<PersonalInfoProjection> existingInfoOpt = inforepository.findByAccountId(AccountId);
            PersonalInfoEntity personalInfoEntity;

            if (existingInfoOpt.isPresent()) {
                personalInfoEntity = new PersonalInfoEntity();
                personalInfoEntity.setAccountId(AccountId);
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
            return "KYC Info sent successfully and saved.";
        } catch (Exception e) {
            log.error("Failed to send KYC Info for accountId: {}", AccountId, e);
            throw new KycProcessingException("Failed to send KYC Info: " + e.getMessage());
        }
    }

    @Override
    public String sendKycLocation(KycLocationRequest kycLocationRequest) {
        if (kycLocationRequest == null || kycLocationRequest.getLocation() == null) {
            throw new IllegalArgumentException("Invalid KYC Location Request");
        }

        try {
            log.info("Processing KYC Location update for accountId: {}", kycLocationRequest.getAccountId());

            String accountId = kycLocationRequest.getAccountId();
            Optional<PersonalInfoProjection> existingInfo = getPersonalInfoAccountId(accountId);
            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = new PersonalInfoEntity();
                personalInfo.setAccountId(accountId);
                personalInfo.setLocation(kycLocationRequest.getLocation());
                inforepository.save(personalInfo);
                log.info("KYC Location updated successfully for accountId: {}", accountId);
                return "KYC Location updated successfully.";
            } else {
                throw new EntityNotFoundException("No KYC record found for the provided accountId.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Location for accountId: {}", kycLocationRequest.getAccountId(), e);
            throw new KycProcessingException("Failed to update KYC Location: " + e.getMessage());
        }
    }

    @Override
    public String sendKycEmail(KycEmailRequest kycEmailRequest) {
        if (kycEmailRequest == null || kycEmailRequest.getEmail() == null) {
            throw new IllegalArgumentException("Invalid KYC Email Request");
        }

        try {
            log.info("Processing KYC Email update for accountId: {}", kycEmailRequest.getAccountId());

            String accountId = kycEmailRequest.getAccountId();
            Optional<PersonalInfoProjection> existingInfo = getPersonalInfoAccountId(accountId);

            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = new PersonalInfoEntity();
                personalInfo.setAccountId(accountId);
                personalInfo.setEmail(kycEmailRequest.getEmail());
                inforepository.save(personalInfo);
                log.info("KYC Email updated successfully for accountId: {}", accountId);
                return "KYC Email updated successfully.";
            } else {
                throw new EntityNotFoundException("No KYC record found for the provided accountId.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Email for accountId: {}", kycEmailRequest.getAccountId(), e);
            throw new KycProcessingException("Failed to update KYC Email: " + e.getMessage());
        }
    }

    @Override
    public Optional<PersonalInfoProjection> getPersonalInfoAccountId(String accountId) {
        return inforepository.findByAccountId(accountId);
    }

    @Override
    public List<UserInfoResponse> getPendingKycRecords() {
        // Get all personal info records with pending status
        List<PersonalInfoProjection> pendingPersonalInfos = inforepository.findByStatus(PersonalInfoStatus.PENDING);
        List<UserInfoResponse> responses = new ArrayList<>();

        for (PersonalInfoProjection info : pendingPersonalInfos) {
            // For each pending personal info, check if there's a matching document
            Optional<UserDocumentsProjection> documentsOpt = repository.findByAccountId(info.getAccountId());

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

        List<PersonalInfoProjection> personalInfoList = inforepository.findByDocumentUniqueId(documentUniqueId);
        List<UserInfoResponse> responseList = new ArrayList<>();

        for (PersonalInfoProjection personalInfo : personalInfoList) {
            try {
                log.info("Processing PersonalInfoProjection: {}", personalInfo);
                Optional<UserDocumentsProjection> documentsOpt = repository.findByAccountId(personalInfo.getAccountId());
                responseList.add(mapToUserInfoResponse(personalInfo, documentsOpt));
            } catch (Exception e) {
                log.error("Error processing PersonalInfoProjection: {}", personalInfo, e);
            }
        }

        log.info("Final UserInfoResponse List: {}", responseList);
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
}