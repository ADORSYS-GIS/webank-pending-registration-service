package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.security.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.jwk.*;
import org.slf4j.*;
import org.springframework.stereotype.*;
import com.adorsys.webank.security.CertGeneratorHelper;
import java.util.*;
import com.adorsys.webank.config.SecurityUtils;
import java.text.ParseException;

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
    public String getCert(String accountId) throws ParseException {
        Optional<PersonalInfoEntity> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);

        if (personalInfoOpt.isPresent() && personalInfoOpt.get().getStatus() == PersonalInfoStatus.APPROVED) {
            ECKey  devicePub = ECKey.parse(SecurityUtils.extractDeviceJwkFromContext());

            try {
                // Convert publicKey to a valid JSON string
                String publicKeyJson = devicePub.toJSONString();
                String certificate = certGeneratorHelper.generateCertificate(publicKeyJson);
                log.info("Certificate generated: {}", certificate);
                return "Your certificate is: " + certificate;
            } catch (Exception e) {
                log.error("Error generating certificate: ", e);
                return "null";
            }
        } else if (personalInfoOpt.isPresent() && personalInfoOpt.get().getStatus() == PersonalInfoStatus.REJECTED) {
            // Get the rejection reason from the entity (assuming getRejectionReason() exists)
            String reason = personalInfoOpt.get().getRejectionReason();
            if (reason == null || reason.isEmpty()) {
                reason = "Your identity verification was rejected. Please check your documents and try again.";
            }
            return "REJECTED: " + reason;
        } else {
            return null;
        }
    }
}