package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.KycCertServiceApi;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

@Service
public class KycCertServiceImpl implements KycCertServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycCertServiceImpl.class);
    private final PersonalInfoRepository personalInfoRepository;

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
    }

    @Override
    @Transactional
    public String getCert(JWK publicKey, String AccountId) {

        Optional<PersonalInfoEntity> personalInfoOpt = personalInfoRepository.findByAccountId(AccountId);

        if (personalInfoOpt.isPresent() && personalInfoOpt.get().getStatus() == PersonalInfoStatus.APPROVED) {
            try {
                String certificate = generateKYCCertificate(publicKey.toJSONString());
                return "Your certificate is: " + certificate;
            } catch (Exception e) {
                log.error("Error generating certificate: ", e);
                return "null";
            }
        }

        return "null";
    }

    private String generateKYCCertificate(String deviceJwkJson) {
        try {
            // Parse the server's private key from the JWK JSON string
            ECKey serverPrivateKey = (ECKey) JWK.parse(SERVER_PRIVATE_KEY_JSON);
            if (serverPrivateKey.getD() == null) {
                throw new IllegalStateException("Private key 'd' (private) parameter is missing.");
            }

            // Signer using server's private key
            JWSSigner signer = new ECDSASigner(serverPrivateKey);

            // Parse the server's public key
            ECKey serverPublicKey = (ECKey) JWK.parse(SERVER_PUBLIC_KEY_JSON);

            // Compute SHA-256 hash of the server’s public JWK to use as `kid`
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serverPublicKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
            String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

            // Create JWT Header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(kid) // Set 'kid' as the SHA-256 of server public JWK
                    .type(JOSEObjectType.JWT)
                    .build();

            // Parse device's public JWK
            JWK deviceJwk = JWK.parse(deviceJwkJson);

            // Create JWT Payload
            long issuedAt = System.currentTimeMillis() / 1000; // Convert to seconds
            log.info("Issued At: " + issuedAt);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)  // Fixed issuer format
                    .audience(deviceJwk.getKeyID()) // Use device public key ID as audience
                    .claim("cnf", Collections.singletonMap("jwk", deviceJwk.toJSONObject())) // Fix JSON structure
                    .issueTime(new Date(issuedAt * 1000))
                    .expirationTime(new Date((issuedAt + (expirationTimeMs / 1000)) * 1000)) // Convert to milliseconds
                    .build();

            // Create JWT token
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            String signedToken = signedJWT.serialize();
            log.info("Generated KYC Certificate: " + signedToken);
            return signedToken;

        } catch (Exception e) {
            throw new IllegalStateException("Error generating device certificate", e);
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
}
