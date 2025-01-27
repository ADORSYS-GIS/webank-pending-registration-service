package com.adorsys.webank.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.proc.BadJWTException;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import static com.adorsys.webank.security.JwtExtractor.extractPayloadHash;
import static com.nimbusds.jose.Payload.Origin.JSON;

@Service
public class JwtValidator {

    public static JWK validateAndExtract(String jwtToken, String... params) throws ParseException, JOSEException, BadJOSEException, NoSuchAlgorithmException, JsonProcessingException {

        // Concatenate all payloads into a single string
        StringBuilder concatenatedPayload = new StringBuilder();
        for (String param : params) {
            concatenatedPayload.append(param);
        }
        String concatenatedPayloadString = concatenatedPayload.toString();

        System.out.println("Timestamp: " + concatenatedPayloadString);

        // Parse the JWS object
        JWSObject jwsObject = JWSObject.parse(jwtToken);

        // Extract the JWK from the header
        System.out.println("JwsObject: " + jwsObject);
        if (!jwsObject.getHeader().toJSONObject().containsKey("jwk")) {
            throw new BadJOSEException("Missing JWK in JWT header.");
        }
        Object jwkObject = jwsObject.getHeader().toJSONObject().get("jwk");
        System.out.println("JwkObject (JSON): " + jwkObject);
        if (jwkObject == null) {
            throw new BadJOSEException("Missing 'jwk' in JWT header");
        }

// Convert to JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        String jwkString = objectMapper.writeValueAsString(jwkObject);


// Parse the JWK
        JWK jwk = JWK.parse(jwkString);

        System.out.println("Timestamp: " + jwk);
        // Ensure it is an EC key (since your frontend uses ES256)
        if (!(jwk instanceof ECKey ecKey)) {
            throw new BadJOSEException("Invalid key type, expected ECKey.");
        }

        // Create a verifier for the EC public key
        var verifier = ecKey.toECPublicKey();

        // Verify the signature
        if (!jwsObject.verify(new ECDSAVerifier(verifier))) {
            throw new BadJWTException("Invalid signature");
        }

        // Extract the payload
        String payload = jwsObject.getPayload().toString();

        String payloadHash = extractPayloadHash(payload);

        if (!payloadHash.equals(hashPayload(concatenatedPayloadString))) {
            throw new BadJWTException("Invalid payload hash");
        }
        return jwk;
    }

    public static String hashPayload(String input) throws NoSuchAlgorithmException {

        // Get a MessageDigest instance for SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Compute the hash for the concatenated payload string
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        // Convert the hash bytes to a hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString(); // Return the hexadecimal string
    }
}
