package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.exceptions.CertificateGenerationException;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycCertServiceApi;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.adorsys.webank.security.CertGeneratorHelper;
import java.util.Optional;

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
        // Find user by accountId, return null if not found
        Optional<PersonalInfoProjection> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);
        if (personalInfoOpt.isEmpty()) {
            logPersonalInfoNotFound(accountId);
            return null;
        }
        
        PersonalInfoProjection personalInfo = personalInfoOpt.get();
        return processPersonalInfoForCertificate(personalInfo, publicKey, accountId);
    }
    
    /**
     * Logs that no personal information was found for the given account ID
     */
    private void logPersonalInfoNotFound(String accountId) {
        if (log.isWarnEnabled()) {
            log.warn("No personal information found for accountId: {}", accountId);
        }
    }
    
    /**
     * Process personal info to generate certificate based on verification status
     */
    private String processPersonalInfoForCertificate(PersonalInfoProjection personalInfo, JWK publicKey, String accountId) {
        switch (personalInfo.getStatus()) {
            case APPROVED:
                return generateCertificateForApproved(publicKey, accountId);
            case REJECTED:
                return generateResponseForRejected(personalInfo, accountId);
            default:
                return handlePendingOrOtherStatus(personalInfo, accountId);
        }
    }
    
    /**
     * Generate certificate for an approved personal info record
     */
    private String generateCertificateForApproved(JWK publicKey, String accountId) {
        try {
            String publicKeyJson = publicKey.toJSONString();
            String certificate = certGeneratorHelper.generateCertificate(publicKeyJson);
            log.debug("Certificate generated successfully for accountId: {}", accountId);
            return "Your certificate is: " + certificate;
        } catch (Exception e) {
            log.error("Error generating certificate for accountId {}: {}", accountId, e.getMessage());
            throw new CertificateGenerationException("Failed to generate KYC certificate: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate response for rejected verification status
     */
    private String generateResponseForRejected(PersonalInfoProjection personalInfo, String accountId) {
        String reason = personalInfo.getRejectionReason();
        if (reason == null || reason.isEmpty()) {
            reason = "Your identity verification was rejected. Please check your documents and try again.";
        }
        log.warn("Certificate request rejected for accountId: {}, reason: {}", accountId, reason);
        return "REJECTED: " + reason;
    }
    
    /**
     * Handle pending or other status scenarios
     */
    private String handlePendingOrOtherStatus(PersonalInfoProjection personalInfo, String accountId) {
        if (log.isInfoEnabled()) {
            log.info("Certificate requested for non-approved account: {}, status: {}", 
                accountId, personalInfo.getStatus());
        }
        return null;
    }
}