package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.CertificateGenerationException;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.jwk.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.*;
import com.adorsys.webank.security.CertGeneratorHelper;

@Service
@Slf4j
public class KycCertServiceImpl implements KycCertServiceApi {

    private final PersonalInfoRepository personalInfoRepository;
    private final CertGeneratorHelper certGeneratorHelper;

    public KycCertServiceImpl(PersonalInfoRepository personalInfoRepository, CertGeneratorHelper certGeneratorHelper) {
        this.personalInfoRepository = personalInfoRepository;
        this.certGeneratorHelper = certGeneratorHelper;
    }

    @Override
    public String getCert(JWK publicKey, String accountId) {
        // Find user by accountId, throw exception if not found
        PersonalInfoEntity personalInfo = personalInfoRepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    log.warn("No personal information found for accountId: {}", accountId);
                    return new IllegalArgumentException("No identity verification record found for this account");
                });

        // Check verification status and generate certificate or return appropriate response
        if (personalInfo.getStatus() == PersonalInfoStatus.APPROVED) {
            try {
                // Convert publicKey to a valid JSON string
                String publicKeyJson = publicKey.toJSONString();
                String certificate = certGeneratorHelper.generateCertificate(publicKeyJson);
                log.debug("Certificate generated successfully for accountId: {}", accountId);
                return "Your certificate is: " + certificate;
            } catch (Exception e) {
                log.error("Error generating certificate for accountId {}: {}", accountId, e.getMessage());
                throw new CertificateGenerationException("Failed to generate KYC certificate: " + e.getMessage(), e);
            }
        } else if (personalInfo.getStatus() == PersonalInfoStatus.REJECTED) {
            // Get the rejection reason from the entity
            String reason = personalInfo.getRejectionReason();
            if (reason == null || reason.isEmpty()) {
                reason = "Your identity verification was rejected. Please check your documents and try again.";
            }
            log.warn("Certificate request rejected for accountId: {}, reason: {}", accountId, reason);
            return "REJECTED: " + reason;
        } else {
            // If status is PENDING or any other status
            log.info("Certificate requested for non-approved account: {}, status: {}", accountId, personalInfo.getStatus());
            return "Your verification is still being processed. Current status: " + personalInfo.getStatus();
        }
    }
}