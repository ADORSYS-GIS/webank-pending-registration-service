package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.security.CertGeneratorHelper;
import com.adorsys.webank.service.KycCertServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class KycCertServiceImpl implements KycCertServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycCertServiceImpl.class);
    private final PersonalInfoRepository personalInfoRepository;
    private final CertGeneratorHelper certGeneratorHelper;

    public KycCertServiceImpl(PersonalInfoRepository personalInfoRepository) {
        this.personalInfoRepository = personalInfoRepository;
        this.certGeneratorHelper = new CertGeneratorHelper();
    }


        @Override
        public String getCert(JWK publicKey, String accountId) {
            Optional<PersonalInfoEntity> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);

            if (personalInfoOpt.isPresent() && personalInfoOpt.get().getStatus() == PersonalInfoStatus.APPROVED) {
                try {
                    // Convert publicKey to a valid JSON string
                    String publicKeyJson = publicKey.toJSONString();
                    String certificate = certGeneratorHelper.generateCertificate(publicKeyJson);
                    log.info("Certificate generated: {}", certificate);
                    return "Your certificate is: " + certificate;
                } catch (Exception e) {
                    log.error("Error generating certificate: ", e);
                    return "null";
                }
            }

            return "null";
        }


}
