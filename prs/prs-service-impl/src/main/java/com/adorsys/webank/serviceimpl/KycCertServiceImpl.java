package com.adorsys.webank.serviceimpl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.adorsys.webank.config.CertGeneratorHelper;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycCertServiceApi;
import com.nimbusds.jose.jwk.ECKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycCertServiceImpl implements KycCertServiceApi {

    private final PersonalInfoRepository personalInfoRepository;
    private final CertGeneratorHelper certGeneratorHelper;

    @Override
    public String getCert(String accountId) {
        Optional<PersonalInfoProjection> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);

        if (personalInfoOpt.isPresent() && personalInfoOpt.get().getStatus() == PersonalInfoStatus.APPROVED) {
            ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();

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
