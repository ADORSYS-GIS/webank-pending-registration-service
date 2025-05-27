package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.jwk.*;
import org.slf4j.*;
import org.springframework.stereotype.*;
import com.adorsys.webank.security.CertGeneratorHelper;
import java.util.*;
import com.adorsys.error.ResourceNotFoundException;
import com.adorsys.error.ValidationException;

@Service
public class KycCertServiceImpl implements KycCertServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycCertServiceImpl.class);
    private final PersonalInfoRepository personalInfoRepository;
    private final CertGeneratorHelper certGeneratorHelper;

    public KycCertServiceImpl(PersonalInfoRepository personalInfoRepository, CertGeneratorHelper certGeneratorHelper) {
        this.personalInfoRepository = personalInfoRepository;
        this.certGeneratorHelper = certGeneratorHelper;
    }

    @Override
    public String getCert(JWK publicKey, String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            throw new ValidationException("Account ID is required");
        }
        Optional<PersonalInfoEntity> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);

        if (personalInfoOpt.isPresent()) {
            PersonalInfoStatus status = personalInfoOpt.get().getStatus();

            // Check status and return appropriate value
            if (status == PersonalInfoStatus.APPROVED) {
                try {
                    // Convert publicKey to a valid JSON string
                    String publicKeyJson = publicKey.toJSONString();
                    String certificate = certGeneratorHelper.generateCertificate(publicKeyJson);
                    log.info("Certificate generated: {}", certificate);
                    return "Your certificate is: " + certificate;
                } catch (Exception e) {
                    log.error("Error generating certificate: ", e);
                    throw new ResourceNotFoundException("Error generating certificate");
                }
            } else if (status == PersonalInfoStatus.REJECTED) {
                return "REJECTED";
            } else if (status == PersonalInfoStatus.PENDING) {
                return "null";
            }
        } else {
            throw new ResourceNotFoundException("No KYC certificate found for account ID: " + accountId);
        }

        return null;
    }
}