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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import static com.adorsys.webank.security.JwtExtractor.extractPayloadHash;

@Service
public class JwtValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);
    private static PasswordEncoder passwordEncoder;
    
    public JwtValidator(PasswordEncoder passwordEncoder) {
        JwtValidator.passwordEncoder = passwordEncoder;
    }

    public static JWK validateAndExtract(String jwtToken, String... params) {
        try {

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
        } catch (ParseException e) {
            logger.error("Failed to parse JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT format", e);
        } catch (JOSEException e) {
            logger.error("JWT processing error: {}", e.getMessage());
            throw new IllegalArgumentException("JWT processing failed: " + e.getMessage(), e) {
                // Add the original exception type as a property
                {
                    this.setStackTrace(e.getStackTrace());
                }
            };
        } catch (BadJOSEException e) {
            logger.error("Invalid JWT: {}", e.getMessage());
            throw new IllegalArgumentException("JWT validation failed: " + e.getMessage(), e) {
                // Add the original exception type as a property
                {
                    this.setStackTrace(e.getStackTrace());
                }
            };
        } catch (NoSuchAlgorithmException e) {
            logger.error("Algorithm error: {}", e.getMessage());
            throw new IllegalStateException("Hashing algorithm unavailable", e);
        } catch (JsonProcessingException e) {
            logger.error("JSON processing error: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JSON format in JWT", e);
        } catch (Exception e) {
            logger.error("Unexpected error during JWT validation: {}", e.getMessage());
            throw new IllegalStateException("JWT validation failed", e);
        }
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
        logger.info("Validating payload hash with Argon2");
        String payloadHash = extractPayloadHash(payload);
        
        // If passwordEncoder is not available, fall back to legacy hash method
        if (passwordEncoder == null) {
            logger.warn("PasswordEncoder not available, falling back to SHA-256");
            String expectedHash = legacyHashPayload(concatenatedPayload);
            logger.debug("Extracted payload hash: {}", payloadHash);
            logger.debug("Expected hash (legacy): {}", expectedHash);
            
            if (!payloadHash.equals(expectedHash)) {
                logger.error("Payload hash validation failed");
                throw new IllegalArgumentException("Invalid payload hash.");
            }
        } else {
            // Use Argon2 for more secure hash verification
            logger.debug("Verifying with Argon2: extracted hash: {}", payloadHash);
            
            // If hash starts with {argon2}, use matches directly
            if (payloadHash.startsWith("{argon2}")) {
                if (!passwordEncoder.matches(concatenatedPayload, payloadHash)) {
                    logger.error("Argon2 payload hash validation failed");
                    throw new BadJWTException("Invalid Argon2 payload hash.");
                }
            } 
            // Otherwise assume it's a legacy hash and compare directly
            else {
                String expectedHash = legacyHashPayload(concatenatedPayload);
                if (!payloadHash.equals(expectedHash)) {
                    logger.error("Legacy payload hash validation failed");
                    throw new BadJWTException("Invalid legacy payload hash.");
                }
            }
        }
        
        logger.info("Payload hash validation successful");
    }

    public static String hashPayload(String input) throws NoSuchAlgorithmException {
        if (passwordEncoder != null) {
            logger.info("Hashing payload using Argon2");
            return passwordEncoder.encode(input);
        } else {
            logger.warn("PasswordEncoder not available, using legacy SHA-256 hashing");
            return legacyHashPayload(input);
        }
    }
    
    // Made public for testing purposes
    public static String legacyHashPayload(String input) throws NoSuchAlgorithmException {
        logger.info("Using legacy SHA-256 hashing");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        logger.debug("Computed legacy hash: {}", hexString);
        return hexString.toString();
    }

    public static String extractClaim(String jwtToken, String claimKey) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwtToken);
            logger.info("Successfully parsed JWT token:{}", signedJWT);
            Object claimValue = signedJWT.getHeader().toJSONObject().get(claimKey);
            if (claimValue == null) {
                logger.warn("Claim '{}' not found in token", claimKey);
                return null;
            }
            return claimValue.toString();
        } catch (ParseException e) {
            logger.error("Error parsing JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Error extracting claim: " + claimKey, e);
        } catch (Exception e) {
            logger.error("Unexpected error extracting claim {}: {}", claimKey, e.getMessage());
            // We handle all exceptions uniformly
            throw new IllegalStateException("Failed to process JWT token", e);
        }
    }

}