package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.dto.KycDocumentRequest;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
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
    public String sendKycDocument( JWK devicePublicKey, KycDocumentRequest kycDocumentRequest) {

        if (kycDocumentRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Document Request");
        }

        try {

            String publicKeyHash = computePublicKeyHash(String.valueOf(devicePublicKey));
            log.info(publicKeyHash);
            UserDocumentsEntity userDocuments = new UserDocumentsEntity();
            userDocuments.setPublicKeyHash(publicKeyHash);
            userDocuments.setFrontID(kycDocumentRequest.getFrontId());
            userDocuments.setBackID(kycDocumentRequest.getBackId());
            userDocuments.setSelfieID(kycDocumentRequest.getSelfieId());
            userDocuments.setTaxID(kycDocumentRequest.getTaxId());
            repository.save(userDocuments);

            return "KYC Document sent successfully and saved";
        } catch (Exception e) {
            log.error("Failed to send KYC Document", e);
            throw new FailedToSendOTPException("Failed to send KYC Document");
        }
    }

    @Override
    public String sendKycinfo( JWK devicePub, KycInfoRequest kycInfoRequest) {
        if (kycInfoRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Info Request");
        }

        try {
            log.info("Processing KYC Info for the device.");

            // Extract public key and compute hash
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            log.info("Computed Public Key Hash: {}", publicKeyHash);

            // Create and populate PersonalInfoEntity
            PersonalInfoEntity personalInfoEntity = PersonalInfoEntity.builder()
                    .publicKeyHash(publicKeyHash)
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
            log.info("KYC Info saved successfully for Public Key Hash: {}", publicKeyHash);

            return "KYC Info sent successfully and saved.";
        } catch (Exception e) {
            log.error("Failed to send KYC Info", e);
            throw new FailedToSendOTPException("Failed to send KYC Info");
        }
    }



    @Override
    public String sendKyclocation(JWK devicePub, KycLocationRequest kycLocationRequest) {
        if (kycLocationRequest == null || kycLocationRequest.getLocation() == null) {
            throw new IllegalArgumentException("Invalid KYC Location Request");
        }

        try {
            log.info("Processing KYC Location update.");

            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            log.info("Computed Public Key Hash: {}", publicKeyHash);


            // Use getPersonalInfoByPublicKey
            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoByPublicKey(publicKeyHash);
            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = existingInfo.get();
                personalInfo.setLocation(kycLocationRequest.getLocation()); // Update location
                inforepository.save(personalInfo);
                log.info("KYC Location updated successfully for Public Key Hash: {}", publicKeyHash);
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
    public String sendKycEmail(JWK devicePub, KycEmailRequest kycEmailRequest) {
        if (kycEmailRequest == null || kycEmailRequest.getEmail() == null) {
            throw new IllegalArgumentException("Invalid KYC Email Request");
        }

        try {
            log.info("Processing KYC Email update.");

            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            log.info("Computed Public Key Hash: {}", publicKeyHash);

            // Use getPersonalInfoByPublicKey
            Optional<PersonalInfoEntity> existingInfo = getPersonalInfoByPublicKey(publicKeyHash);

            if (existingInfo.isPresent()) {
                PersonalInfoEntity personalInfo = existingInfo.get();
                personalInfo.setEmail(kycEmailRequest.getEmail()); // Update email
                inforepository.save(personalInfo);
                log.info("KYC Email updated successfully for Public Key Hash: {}", publicKeyHash);
                return "KYC Email updated successfully.";
            } else {
                throw new EntityNotFoundException("No KYC record found for the provided Public Key Hash.");
            }
        } catch (Exception e) {
            log.error("Failed to update KYC Email", e);
            throw new FailedToSendOTPException("Failed to update KYC Email");
        }
    }



    public String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing hash");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public Optional<UserDocumentsEntity> getDocuments(String publicKeyHash) {
        return repository.findById(publicKeyHash);
    }

    @Override
    public Optional<PersonalInfoEntity> getPersonalInfoByPublicKey(String publicKeyHash) {
        return inforepository.findByPublicKeyHash(publicKeyHash);
    }

    @Override
    public List<PersonalInfoEntity> getPersonalInfoByStatus(PersonalInfoStatus status) {
        return inforepository.findByStatus(status);
    }

    private String computePublicKeyHash(String devicePublicKey) {
        return computeHash(devicePublicKey);
    }

}
