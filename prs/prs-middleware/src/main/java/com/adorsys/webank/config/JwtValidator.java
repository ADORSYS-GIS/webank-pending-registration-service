package com.adorsys.webank.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.adorsys.webank.exceptions.SecurityConfigurationException;
import com.adorsys.webank.exceptions.JwtPayloadParseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import static com.adorsys.webank.config.JwtExtractor.extractPayloadHash;

@Service
public class JwtValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);

    public static JWK validateAndExtract(String jwtToken, String... params)
            throws ParseException, JOSEException, BadJOSEException, NoSuchAlgorithmException, JsonProcessingException {

        logger.info("Starting JWT validation");
        String concatenatedPayload = concatenatePayloads(params);
        logger.debug("Concatenated payload: {}", concatenatedPayload);

        JWSObject jwsObject = JWSObject.parse(jwtToken);
        logger.info("Parsed JWSObject successfully");

        JWK jwk = extractAndValidateJWK(jwsObject);
        logger.info("Extracted and validated JWK successfully");

        verifySignature(jwsObject, (ECKey) jwk);
        logger.info("JWT signature verification passed");

        validatePayloadHash(jwsObject.getPayload().toString(), concatenatedPayload);
        logger.info("Payload hash validation passed");
        return jwk;

    }


    private static String concatenatePayloads(String... params) {
        logger.debug("Concatenating payload parameters");
        StringBuilder concatenatedPayload = new StringBuilder();
        for (String param : params) {
            concatenatedPayload.append(param);
        }
        logger.debug("Payload: {}", concatenatedPayload);
        return concatenatedPayload.toString();
    }

    private static JWK extractAndValidateJWK(JWSObject jwsObject)
            throws BadJOSEException, JsonProcessingException, ParseException {
        logger.info("Extracting JWK from JWT header");
        Object jwkObject = jwsObject.getHeader().toJSONObject().get("jwk");
        if (jwkObject == null) {
            logger.error("Missing 'jwk' in JWT header");
            throw new SecurityConfigurationException("Missing 'jwk' in JWT header", null);
        }

        String jwkString = new ObjectMapper().writeValueAsString(jwkObject);
        logger.debug("Parsed JWK string: {}", jwkString);

        JWK jwk = JWK.parse(jwkString);
        if (!(jwk instanceof ECKey)) {
            logger.error("Invalid key type, expected ECKey but found {}", jwk.getKeyType());
            throw new SecurityConfigurationException("Invalid key type, expected ECKey", null);
        }

        logger.info("Successfully validated JWK");
        return jwk;
    }

    private static void verifySignature(JWSObject jwsObject, ECKey ecKey)
            throws JOSEException, BadJWTException {
        logger.info("Verifying JWT signature");
        var verifier = ecKey.toECPublicKey();
        if (!jwsObject.verify(new ECDSAVerifier(verifier))) {
            logger.error("Invalid signature detected");
            throw new SecurityConfigurationException("Invalid signature", null);
        }
        logger.info("Signature verification successful");
    }

    private static void validatePayloadHash(String payload, String concatenatedPayload)
            throws NoSuchAlgorithmException, BadJWTException {
        logger.info("Validating payload hash");
        String payloadHash = extractPayloadHash(payload);
        String expectedHash = hashPayload(concatenatedPayload);

        logger.debug("Extracted payload hash: {}", payloadHash);
        logger.debug("Expected hash: {}", expectedHash);

        if (!payloadHash.equals(expectedHash)) {
            logger.error("Payload hash validation failed");
            throw new SecurityConfigurationException("Invalid payload hash", null);
        }
        logger.info("Payload hash validation successful");
    }

    public static String hashPayload(String input) throws NoSuchAlgorithmException {
        logger.info("Hashing payload using SHA-256");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        logger.debug("Computed hash: {}", hexString);
        return hexString.toString();
    }

    public static String extractClaim(String jwtToken, String claimKey) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwtToken);
            logger.info("Successfully parsed JWT token:{}", signedJWT);
            return signedJWT.getHeader().toJSONObject().get(claimKey).toString();
        } catch (ParseException e) {
            throw new JwtPayloadParseException("Error extracting claim: " + claimKey, e);
        }
    }

    /**
     * Extracts the device JWK from an already-validated JWT token.
     * This method assumes the JWT has already been validated by Spring Security.
     *
     * @param jwtToken The validated JWT token
     * @return The device public key as a JSON string
     * @throws IllegalArgumentException if extraction fails
     */
    public static ECKey extractDeviceJwk(String jwtToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwtToken);
            Object jwkObject = signedJWT.getHeader().toJSONObject().get("jwk");
            if (jwkObject == null) {
                throw new SecurityConfigurationException("Missing 'jwk' in JWT header", null);
            }

            String jwkJson = new ObjectMapper().writeValueAsString(jwkObject);
            return ECKey.parse(jwkJson);
        } catch (Exception e) {
            throw new SecurityConfigurationException("Failed to extract or parse device JWK from JWT", e);
        }
    }
}