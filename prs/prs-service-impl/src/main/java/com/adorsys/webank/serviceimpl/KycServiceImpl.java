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
            throw new IllegalArgumentException("Invalid KYC Document Request");
        }

        try {
            log.info("Processing KYC Document for accountId: {}", AccountId);

            // Build the UserDocumentsEntity using builder pattern
            UserDocumentsEntity userDocuments = UserDocumentsEntity.builder()
                    .accountId(AccountId)
                    .frontID(kycDocumentRequest.getFrontId())
                    .backID(kycDocumentRequest.getBackId())
                    .selfieID(kycDocumentRequest.getSelfieId())
                    .taxID(kycDocumentRequest.getTaxId())
                    .build();

            repository.save(userDocuments);
            return "KYC Document sent successfully and saved";
        } catch (Exception e) {
            log.error("Failed to send KYC Document", e);
            throw new FailedToSendOTPException("Failed to send KYC Document");
        }
    }

    @Override
    public String sendKycinfo( String AccountId, KycInfoRequest kycInfoRequest) {
        if (kycInfoRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Info Request");
        }

        try {
            log.info("Processing KYC Info for the device.");

            // Create and populate PersonalInfoEntity
            PersonalInfoEntity personalInfoEntity = PersonalInfoEntity.builder()
                    .accountId(AccountId)
                    .documentUniqueId(kycInfoRequest.getIdNumber())
                    .expirationDate(kycInfoRequest.getExpiryDate())
                    .status(PersonalInfoStatus.PENDING)
                    .build();

            // Save entity in the database
            inforepository.save(personalInfoEntity);

            return "KYC Info sent successfully and saved.";
        } catch (Exception e) {
            log.error("Failed to send KYC Info", e);
            throw new FailedToSendOTPException("Failed to send KYC Info");
        }
    }


    @Override
    public String sendKyclocation( KycLocationRequest kycLocationRequest) {
        if (kycLocationRequest == null || kycLocationRequest.getLocation() == null) {
            throw new IllegalArgumentException("Invalid KYC Location Request");
        }

        try {
            log.info("Processing KYC Location update.");

            String accountId = kycLocationRequest.getAccountId();
            // Use getPersonalInfoByPublicKey
            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoAccountId(accountId);
            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = existingInfo.get();
                personalInfo.setLocation(kycLocationRequest.getLocation()); // Update location
                inforepository.save(personalInfo);
                log.info("KYC Location updated successfully for accountId: {}", accountId);
                return "KYC Location updated successfully.";
            } else {
                throw new EntityNotFoundException("No KYC record found for the provided Public Key Hash.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Location", e);
            throw new FailedToSendOTPException("Failed to update KYC Location");
        }
    }


    @Override
    public String sendKycEmail( KycEmailRequest kycEmailRequest) {
        if (kycEmailRequest == null || kycEmailRequest.getEmail() == null) {
            throw new IllegalArgumentException("Invalid KYC Email Request");
        }

        try {
            log.info("Processing KYC Email update.");

            String accountId = kycEmailRequest.getAccountId();
            // Use getPersonalInfoAccountId
            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoAccountId(accountId);

            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = existingInfo.get();
                personalInfo.setEmail(kycEmailRequest.getEmail()); // Update email
                inforepository.save(personalInfo);
                log.info("KYC Email updated successfully for accountId: {}", accountId);
                return "KYC Email updated successfully.";
            } else {
                throw new EntityNotFoundException("No KYC record found for the provided Public Key Hash.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Email", e);
            throw new FailedToSendOTPException("Failed to update KYC Email");
        }
    }

    @Override
    public Optional<PersonalInfoEntity> getPersonalInfoAccountId(String accountId) {
        return inforepository.findByAccountId(accountId);
    }

    @Override
    public List<UserInfoResponse> getPendingKycRecords() {
        List<PersonalInfoEntity> pendingInfos = inforepository.findByStatus(PersonalInfoStatus.PENDING);
        List<UserInfoResponse> responses = new ArrayList<>();

        for (PersonalInfoEntity info : pendingInfos) {
            Optional<UserDocumentsEntity> documentsOpt = repository.findByAccountId(info.getAccountId());
            responses.add(mapToUserInfoResponse(info, documentsOpt));
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


    //Helper Method
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

        return response;
    }


}