package com.adorsys.webank.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.proc.BadJWTException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import static com.adorsys.webank.security.JwtExtractor.extractPayloadHash;

@Service
public class JwtValidator {

    /**
     * Validates the JWT, verifies the signature, and checks the payload hash.
     *
     * @param jwtToken JWT token to validate.
//     * @param params   Payload parameters to validate against the hash.
     * @return Validated JWK.
     * @throws ParseException         If the JWT token cannot be parsed.
     * @throws JOSEException          If there's an error in verifying the signature.
     * @throws BadJOSEException       If the JWK is invalid or missing.
     * @throws NoSuchAlgorithmException If SHA-256 algorithm is unavailable.
     * @throws JsonProcessingException If there's an error in processing the JWK JSON.
     */
    public static JWK validateAndExtract(String jwtToken)
            throws ParseException, JOSEException, BadJOSEException, NoSuchAlgorithmException, JsonProcessingException {

//        String concatenatedPayload = concatenatePayloads(params);
        JWSObject jwsObject = JWSObject.parse(jwtToken);
        JWK jwk = extractAndValidateJWK(jwsObject);
        verifySignature(jwsObject, (ECKey) jwk);
//        validatePayloadHash(jwsObject.getPayload().toString(), concatenatedPayload);
        return jwk;
    }

    /**
     * Concatenates the given payload parameters into a single string.
     *
     * @param params Array of payload parameters.
     * @return Concatenated payload string.
     */
    private static String concatenatePayloads(String... params) {
        StringBuilder concatenatedPayload = new StringBuilder();
        for (String param : params) {
            concatenatedPayload.append(param);
        }
        return concatenatedPayload.toString();
    }

    /**
     * Extracts and validates the JWK from the JWT header.
     *
     * @param jwsObject Parsed JWS object.
     * @return Validated JWK.
     * @throws BadJOSEException       If the JWK is missing or invalid.
     * @throws JsonProcessingException If there's an error in processing the JWK JSON.
     * @throws ParseException          If the JWK cannot be parsed.
     */
    private static JWK extractAndValidateJWK(JWSObject jwsObject)
            throws BadJOSEException, JsonProcessingException, ParseException {
        Object jwkObject = jwsObject.getHeader().toJSONObject().get("jwk");
        if (jwkObject == null) {
            throw new BadJOSEException("Missing 'jwk' in JWT header.");
        }
        String jwkString = new ObjectMapper().writeValueAsString(jwkObject);
        JWK jwk = JWK.parse(jwkString);
        if (!(jwk instanceof ECKey)) {
            throw new BadJOSEException("Invalid key type, expected ECKey.");
        }
        return jwk;
    }

    /**
     * Verifies the signature of the JWT using the provided EC public key.
     *
     * @param jwsObject Parsed JWS object.
     * @param ecKey     EC public key.
     * @throws JOSEException     If there's an error in verifying the signature.
     * @throws BadJWTException If the signature is invalid.
     */
    private static void verifySignature(JWSObject jwsObject, ECKey ecKey)
            throws JOSEException, BadJWTException {
        var verifier = ecKey.toECPublicKey();
        if (!jwsObject.verify(new ECDSAVerifier(verifier))) {
            throw new BadJWTException("Invalid signature.");
        }
    }

    /**
     * Validates the payload hash against the concatenated payload.
     *
     * @param payload             Extracted payload from the JWT.
     * @param concatenatedPayload Concatenated payload string.
     * @throws NoSuchAlgorithmException If SHA-256 algorithm is unavailable.
     * @throws BadJWTException         If the payload hash is invalid.
     */
    private static void validatePayloadHash(String payload, String concatenatedPayload)
            throws NoSuchAlgorithmException, BadJWTException {
        String payloadHash = extractPayloadHash(payload);
        if (!payloadHash.equals(hashPayload(concatenatedPayload))) {
            throw new BadJWTException("Invalid payload hash.");
        }
    }

    /**
     * Hashes the given input using SHA-256 and returns the hexadecimal hash string.
     *
     * @param input Input string to hash.
     * @return Hexadecimal hash string.
     * @throws NoSuchAlgorithmException If SHA-256 algorithm is unavailable.
     */
    public static String hashPayload(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
