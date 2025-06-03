package com.adorsys.webank.security;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import static com.adorsys.webank.security.JwtExtractor.extractPayloadHash;

@Service
public class JwtValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);
    private static HashHelper hashHelper;

    @Autowired
    public JwtValidator(HashHelper hashHelper) {
        JwtValidator.hashHelper = hashHelper;
        logger.info("JwtValidator initialized with HashHelper");
    }

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
            throw new BadJOSEException("Missing 'jwk' in JWT header.");
        }

        String jwkString = new ObjectMapper().writeValueAsString(jwkObject);
        logger.debug("Parsed JWK string: {}", jwkString);

        JWK jwk = JWK.parse(jwkString);
        if (!(jwk instanceof ECKey)) {
            logger.error("Invalid key type, expected ECKey but found {}", jwk.getKeyType());
            throw new BadJOSEException("Invalid key type, expected ECKey.");
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
            throw new BadJWTException("Invalid signature.");
        }
        logger.info("Signature verification successful");
    }

    private static void validatePayloadHash(String payload, String concatenatedPayload)
            throws NoSuchAlgorithmException, BadJWTException {
        logger.info("Validating payload hash");
        String payloadHash = extractPayloadHash(payload);
        String expectedHash = hashHelper.hashPayload(concatenatedPayload);

        logger.debug("Extracted payload hash: {}", payloadHash);
        logger.debug("Expected hash: {}", expectedHash);

        if (!payloadHash.equals(expectedHash)) {
            logger.error("Payload hash validation failed");
            throw new BadJWTException("Invalid payload hash.");
        }
        logger.info("Payload hash validation successful");
    }

    public static String extractClaim(String jwtToken, String claimKey) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwtToken);
            logger.info("Successfully parsed JWT token:{}", signedJWT);
            return signedJWT.getHeader().toJSONObject().get(claimKey).toString();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error extracting claim: " + claimKey, e);
        }
    }

}