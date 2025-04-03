package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class KycStatusUpdateServiceImpl implements KycStatusUpdateServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycStatusUpdateServiceImpl.class);
    private final PersonalInfoRepository inforepository;

    public KycStatusUpdateServiceImpl(PersonalInfoRepository inforepository) {
        this.inforepository = inforepository;
    }

    @Override
    @Transactional
    public String updateKycStatus(String publicKeyHash, String newStatus) {
        log.info("Updating KYC status for publicKeyHash {} to {}", publicKeyHash, newStatus);

        Optional<PersonalInfoEntity> personalInfoOpt = inforepository.findByAccountId(publicKeyHash);
        if (personalInfoOpt.isPresent()) {
            PersonalInfoEntity personalInfo = personalInfoOpt.get();

            try {
                // Convert newStatus string to Enum
                PersonalInfoStatus kycStatus = PersonalInfoStatus.valueOf(newStatus.toUpperCase());
                personalInfo.setStatus(kycStatus);  // Update status field

                inforepository.save(personalInfo); // Save the updated record

                log.info("Successfully updated KYC status for publicKeyHash {}", publicKeyHash);
                return "KYC status for " + publicKeyHash + " updated to " + newStatus;
            } catch (IllegalArgumentException e) {
                log.error("Invalid KYC status value: {}", newStatus);
                return "Failed: Invalid KYC status value '" + newStatus + "'";
            }
        } else {
            log.warn("No record found for publicKeyHash {}", publicKeyHash);
            return "Failed: No record found for publicKeyHash " + publicKeyHash;
        }
    }

    public Optional<PersonalInfoEntity> getPersonalInfoByPublicKey(String publicKeyHash) {
        return inforepository.findByAccountId(publicKeyHash);
    }
}
