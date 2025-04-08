package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.repository.UserDocumentsRepository;
import com.adorsys.webank.service.KycServiceApi;
import com.nimbusds.jose.jwk.JWK;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class KycServiceImpl implements KycServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycServiceImpl.class);
    private final UserDocumentsRepository repository;
    private final PersonalInfoRepository inforepository;

    public KycServiceImpl(UserDocumentsRepository repository, PersonalInfoRepository inforepository) {
        this.repository = repository;
        this.inforepository = inforepository;
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
                    .documentUniqueId(kycInfoRequest.getIdNumber())  // Mapping idNumber -> documentUniqueId
                    .name(kycInfoRequest.getFullName())              // Mapping fullName -> name
                    .dateOfBirth(kycInfoRequest.getDateOfBirth())
                    .profession(kycInfoRequest.getProfession())
                    .region(kycInfoRequest.getCurrentRegion())       // Mapping currentRegion -> region
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
            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoByPublicKey(accountId);
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
            // Use getPersonalInfoByPublicKey
            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoByPublicKey(accountId);

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
    public Optional<UserDocumentsEntity> getDocuments(String accountId) {
        return repository.findById(accountId);
    }

    @Override
    public Optional<PersonalInfoEntity> getPersonalInfoByPublicKey(String accountId) {
        return inforepository.findByAccountId(accountId);
    }

    @Override
    public List<PersonalInfoEntity> getPersonalInfoByStatus(PersonalInfoStatus status) {
        return inforepository.findByStatus(status);
    }

    @Override
    public List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId) {
        List<PersonalInfoEntity> personalInfoList = inforepository.findByDocumentUniqueId(documentUniqueId);

        List<UserInfoResponse> responseList = new ArrayList<>();

        for (PersonalInfoEntity personalInfo : personalInfoList) {
            String accountId = personalInfo.getAccountId();

            Optional<UserDocumentsEntity> documentsOpt = repository.findById(accountId);
            UserDocumentsEntity documents = documentsOpt.orElse(new UserDocumentsEntity());

            UserInfoResponse response = new UserInfoResponse();
            response.setAccountId(accountId);
            response.setFullName(personalInfo.getName());
            response.setProfession(personalInfo.getProfession());
            response.setIdNumber(personalInfo.getDocumentUniqueId());
            response.setDob(personalInfo.getDateOfBirth());
            response.setRegion(personalInfo.getRegion());
            response.setExpirationDate(personalInfo.getExpirationDate());
            response.setLocation(personalInfo.getLocation());
            response.setEmail(personalInfo.getEmail());
            response.setStatus(personalInfo.getStatus().name()); // Converts Enum to String


            // Set documents
            response.setFrontID(documents.getFrontID());
            response.setBackID(documents.getBackID());
            response.setSelfie(documents.getSelfieID());
            response.setTaxDocument(documents.getTaxID());

            responseList.add(response);
        }

        return responseList;
    }



}