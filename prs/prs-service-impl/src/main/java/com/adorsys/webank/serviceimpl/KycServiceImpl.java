package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycServiceApi;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class KycServiceImpl implements KycServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycServiceImpl.class);
    private final PersonalInfoRepository inforepository;

    public KycServiceImpl(PersonalInfoRepository inforepository) {
        this.inforepository = inforepository;
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


            UserInfoResponse response = new UserInfoResponse();
            response.setAccountId(accountId);
            response.setIdNumber(personalInfo.getDocumentUniqueId());
            response.setExpirationDate(personalInfo.getExpirationDate());
            response.setLocation(personalInfo.getLocation());
            response.setEmail(personalInfo.getEmail());
            response.setStatus(personalInfo.getStatus().name());

            responseList.add(response);
        }

        return responseList;
    }


}