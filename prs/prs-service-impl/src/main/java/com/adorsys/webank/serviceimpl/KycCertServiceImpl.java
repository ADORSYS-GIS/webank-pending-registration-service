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

    @Value("${server.private.key.json}")
    private String SERVER_PRIVATE_KEY_JSON;

    @Value("${server.public.key.json}")
    private String SERVER_PUBLIC_KEY_JSON;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-time-ms}")
    private Long expirationTimeMs;

    public KycCertServiceImpl(PersonalInfoRepository personalInfoRepository) {
        this.personalInfoRepository = personalInfoRepository;
        this.certGeneratorHelper = new CertGeneratorHelper(SERVER_PRIVATE_KEY_JSON, SERVER_PUBLIC_KEY_JSON, issuer, expirationTimeMs);
    }

    @Override
    @Transactional
    public String getCert(JWK publicKey) {
        String publicKeyHash = computeHash(String.valueOf(publicKey));

        Optional<PersonalInfoEntity> personalInfoOpt = personalInfoRepository.findByPublicKeyHash(publicKeyHash);

        if (personalInfoOpt.isPresent() && personalInfoOpt.get().getStatus() == PersonalInfoStatus.APPROVED) {
            try {
                String certificate = certGeneratorHelper.generateCertificate(publicKey.toJSONString());
                return "Your certificate is: " + certificate;
            } catch (Exception e) {
                log.error("Error generating certificate: ", e);
                return "null";
            }
        }

        return "null";
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
}
