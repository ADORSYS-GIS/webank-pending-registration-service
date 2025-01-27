package com.adorsys.webank.security;

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

    public static void validateAndExtract(String jwtToken, String... params) throws ParseException, JOSEException, BadJOSEException, NoSuchAlgorithmException {

        // Concatenate all payloads into a single string
        StringBuilder concatenatedPayload = new StringBuilder();
        for (String payload : params) {
            concatenatedPayload.append(payload);
        }
        String concatenatedPayloadString = concatenatedPayload.toString();

        // Parse the JWS object
        JWSObject jwsObject = JWSObject.parse(jwtToken);

        // Extract the JWK from the header
        String jwkString = jwsObject.getHeader().toJSONObject().get("jwk").toString();
        JWK jwk = JWK.parse(jwkString);

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

        // Print header and payload for demonstration
        System.out.println("Header: " + jwsObject.getHeader().toJSONObject());
        System.out.println("Payload: " + payload);
        System.out.println("Timestamp: " + concatenatedPayloadString);

        String payloadHash = extractPayloadHash(payload);
        System.out.println("Payload Hash: " + payloadHash);

        System.out.println("Hashed body: " + hashPayload(concatenatedPayloadString));

        if (!payloadHash.equals(hashPayload(concatenatedPayloadString))) {
            throw new BadJWTException("Invalid payload hash");
        }
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
