package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
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
            log.warn("Invalid KYC Document Request received for accountId: {}", maskAccountId(AccountId));
            throw new IllegalArgumentException("Invalid KYC Document Request");
        }

        try {
            log.info("Processing KYC Document for accountId: {}", maskAccountId(AccountId));

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
                log.debug("Updating existing document for accountId: {}", maskAccountId(AccountId));
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
                log.debug("Creating new document for accountId: {}", maskAccountId(AccountId));
            }

            // Save to DB
            repository.save(userDocuments);
            log.info("KYC Document saved successfully for accountId: {}", maskAccountId(AccountId));
            return "KYC Document sent successfully and saved";
        } catch (Exception e) {
            log.error("Failed to send KYC Document for accountId: {}", maskAccountId(AccountId), e);
            throw new KycProcessingException("Failed to send KYC Document: " + e.getMessage());
        }
    }

    @Override
    public String sendKycInfo(String AccountId, KycInfoRequest kycInfoRequest) {
        if (kycInfoRequest == null) {
            log.warn("Invalid KYC Info Request received for accountId: {}", maskAccountId(AccountId));
            throw new IllegalArgumentException("Invalid KYC Info Request");
        }

        try {
            log.info("Processing KYC Info for accountId: {}", maskAccountId(AccountId));
            
            // Log masked sensitive information
            if (log.isDebugEnabled()) {
                log.debug("Processing ID number: {}, expiry date: {}", 
                    maskIdNumber(kycInfoRequest.getIdNumber()), 
                    kycInfoRequest.getExpiryDate());
            }

            Optional<PersonalInfoEntity> existingInfoOpt = inforepository.findByAccountId(AccountId);
            PersonalInfoEntity personalInfoEntity;

            if (existingInfoOpt.isPresent()) {
                personalInfoEntity = existingInfoOpt.get();
                personalInfoEntity.setDocumentUniqueId(kycInfoRequest.getIdNumber());
                personalInfoEntity.setExpirationDate(kycInfoRequest.getExpiryDate());
                personalInfoEntity.setStatus(PersonalInfoStatus.PENDING);
                log.debug("Updating existing personal info for accountId: {}", maskAccountId(AccountId));
            } else {
                personalInfoEntity = PersonalInfoEntity.builder()
                        .accountId(AccountId)
                        .documentUniqueId(kycInfoRequest.getIdNumber())
                        .expirationDate(kycInfoRequest.getExpiryDate())
                        .status(PersonalInfoStatus.PENDING)
                        .build();
                log.debug("Creating new personal info for accountId: {}", maskAccountId(AccountId));
            }

            inforepository.save(personalInfoEntity);
            log.info("KYC Info saved successfully for accountId: {}", maskAccountId(AccountId));
            return "KYC Info sent successfully and saved.";
        } catch (Exception e) {
            log.error("Failed to send KYC Info for accountId: {}", maskAccountId(AccountId), e);
            throw new KycProcessingException("Failed to send KYC Info: " + e.getMessage());
        }
    }

    @Override
    public String sendKycLocation(KycLocationRequest kycLocationRequest) {
        if (kycLocationRequest == null || kycLocationRequest.getLocation() == null) {
            log.warn("Invalid KYC Location Request received");
            throw new IllegalArgumentException("Invalid KYC Location Request");
        }

        String accountId = kycLocationRequest.getAccountId();
        try {
            log.info("Processing KYC Location update for accountId: {}", maskAccountId(accountId));
            log.debug("Location: {}", kycLocationRequest.getLocation());

            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoAccountId(accountId);
            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = existingInfo.get();
                personalInfo.setLocation(kycLocationRequest.getLocation());
                inforepository.save(personalInfo);
                log.info("KYC Location updated successfully for accountId: {}", maskAccountId(accountId));
                return "KYC Location updated successfully.";
            } else {
                log.warn("No KYC record found for accountId: {}", maskAccountId(accountId));
                throw new EntityNotFoundException("No KYC record found for the provided accountId.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Location for accountId: {}", maskAccountId(accountId), e);
            throw new KycProcessingException("Failed to update KYC Location: " + e.getMessage());
        }
    }

    @Override
    public String sendKycEmail(KycEmailRequest kycEmailRequest) {
        if (kycEmailRequest == null || kycEmailRequest.getEmail() == null) {
            log.warn("Invalid KYC Email Request received");
            throw new IllegalArgumentException("Invalid KYC Email Request");
        }

        String accountId = kycEmailRequest.getAccountId();
        try {
            log.info("Processing KYC Email update for accountId: {}", maskAccountId(accountId));
            log.debug("Email: {}", maskEmail(kycEmailRequest.getEmail()));

            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoAccountId(accountId);

            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = existingInfo.get();
                personalInfo.setEmail(kycEmailRequest.getEmail());
                inforepository.save(personalInfo);
                log.info("KYC Email updated successfully for accountId: {}", maskAccountId(accountId));
                return "KYC Email updated successfully.";
            } else {
                log.warn("No KYC record found for accountId: {}", maskAccountId(accountId));
                throw new EntityNotFoundException("No KYC record found for the provided accountId.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Email for accountId: {}", maskAccountId(accountId), e);
            throw new KycProcessingException("Failed to update KYC Email: " + e.getMessage());
        }
    }

    @Override
    public Optional<PersonalInfoEntity> getPersonalInfoAccountId(String accountId) {
        log.debug("Retrieving personal info for accountId: {}", maskAccountId(accountId));
        return inforepository.findByAccountId(accountId);
    }

    @Override
    public List<UserInfoResponse> getPendingKycRecords() {
        log.info("Retrieving all pending KYC records");
        
        // Get all personal info records with pending status
        List<PersonalInfoEntity> pendingPersonalInfos = inforepository.findByStatus(PersonalInfoStatus.PENDING);
        log.debug("Found {} pending personal info records", pendingPersonalInfos.size());
        
        List<UserInfoResponse> responses = new ArrayList<>();

        for (PersonalInfoEntity info : pendingPersonalInfos) {
            // For each pending personal info, check if there's a matching document
            Optional<UserDocumentsEntity> documentsOpt = repository.findByAccountId(info.getAccountId());

            // Only add to response if user document exists and is pending
            if (documentsOpt.isPresent() && documentsOpt.get().getStatus() == UserDocumentsStatus.PENDING) {
                responses.add(mapToUserInfoResponse(info, documentsOpt));
                log.debug("Added record for accountId: {} to response", maskAccountId(info.getAccountId()));
            }
        }

        log.info("Returning {} pending KYC records", responses.size());
        return responses;
    }

    @Override
    public List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId) {
        log.info("Finding KYC records for documentUniqueId: {}", maskIdNumber(documentUniqueId));

        List<PersonalInfoEntity> personalInfoList = inforepository.findByDocumentUniqueId(documentUniqueId);
        log.debug("Found {} personal info records with the given document ID", personalInfoList.size());
        
        List<UserInfoResponse> responseList = new ArrayList<>();

        for (PersonalInfoEntity personalInfo : personalInfoList) {
            try {
                log.debug("Processing record for accountId: {}", maskAccountId(personalInfo.getAccountId()));
                Optional<UserDocumentsEntity> documentsOpt = repository.findByAccountId(personalInfo.getAccountId());
                responseList.add(mapToUserInfoResponse(personalInfo, documentsOpt));
            } catch (Exception e) {
                log.error("Error processing record for accountId: {}", 
                    maskAccountId(personalInfo.getAccountId()), e);
            }
        }

        log.info("Returning {} KYC records", responseList.size());
        return responseList;
    }

    private UserInfoResponse mapToUserInfoResponse(PersonalInfoEntity info, Optional<UserDocumentsEntity> documentsOpt) {
        UserDocumentsEntity documents = documentsOpt.orElseGet(UserDocumentsEntity::new);

        UserInfoResponse response = new UserInfoResponse();
        response.setAccountId(info.getAccountId());
        response.setIdNumber(info.getDocumentUniqueId());
        response.setExpirationDate(info.getExpirationDate());
        response.setLocation(info.getLocation());
        response.setEmail(info.getEmail());
        response.setStatus(info.getStatus().name());

        response.setFrontID(documents.getFrontID());
        response.setBackID(documents.getBackID());
        response.setSelfie(documents.getSelfieID());
        response.setTaxDocument(documents.getTaxID());

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
        if (idNumber == null || idNumber.length() < 5) {
            return "********";
        }
        return idNumber.substring(0, 2) + "****" + idNumber.substring(idNumber.length() - 2);
    }
    
    /**
     * Masks an email address for logging purposes
     * Shows only first character and domain
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "********";
        }
        
        if (email.contains("@")) {
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                String firstChar = email.substring(0, 1);
                String domain = email.substring(atIndex);
                return firstChar + "****" + domain;
            }
        }
        
        return email.substring(0, 1) + "********";
    }
}