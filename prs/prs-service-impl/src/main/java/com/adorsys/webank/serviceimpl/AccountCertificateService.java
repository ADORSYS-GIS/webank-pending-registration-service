package com.adorsys.webank.serviceimpl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class AccountCertificateService {
    private static final Logger log = LoggerFactory.getLogger(AccountCertificateService.class);
    private static  final long expirationDays = 30;

    @Value("${server.private.key}")
    private String serverPrivateKeyJson;

    public String generateBankAccountCertificate(String devicePublicKey, String accountId) {
        try {

            // Parse the server's private key from the JWK JSON string
            ECKey serverPrivateKey = (ECKey) JWK.parse(serverPrivateKeyJson);

            // Check that the private key contains the 'd' (private) parameter for signing
            if (serverPrivateKey.getD() == null) {
                throw new IllegalStateException("Private key 'd' (private) parameter is missing.");
            }

            JWSSigner signer = new ECDSASigner(serverPrivateKey);

            // Compute hash of the device's public key
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedDevicePubKey = digest.digest(devicePublicKey.getBytes(StandardCharsets.UTF_8));
            String devicePubKeyHash = Base64.getEncoder().encodeToString(hashedDevicePubKey);

            // Compute hash of the account ID
            byte[] hashedAccountId = digest.digest(accountId.getBytes(StandardCharsets.UTF_8));
            String accountIdHash = HashUtil.hashToHex(hashedAccountId);

            // Calculate expiration timestamp
            long expirationTime = Instant.now().plusSeconds(expirationDays * 86400).getEpochSecond();
            // Create JWT payload including phoneHash, devicePubKeyHash, and accountIdHash
            String payloadData = String.format("{\"acc\": \"%s\", \"exp\": %d, \"cnf\": \"%s\"}", accountIdHash, expirationTime, devicePubKeyHash);
            Payload payload = new Payload(payloadData);

            // Create the JWT header with the JWK object (the server public key)
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType.JWT)
                    .build();

            // Build the JWS object
            JWSObject jwsObject = new JWSObject(header, payload);
            jwsObject.sign(signer);

            return jwsObject.serialize();
        } catch (Exception e) {
            // Log the exception for debugging
            log.error("Error generating device certificate", e);
            throw new IllegalStateException("Error generating device certificate");
        }
    }

    public static class HashUtil {
        //Private constructor to prevent instantiation
        private HashUtil() {
        }

        public static String hashToHex(byte[] hashedBytes) {
            // Convert to Hex encoding
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        }
    }
}
