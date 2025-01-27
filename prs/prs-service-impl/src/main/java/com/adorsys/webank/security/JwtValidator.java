package com.adorsys.webank.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.proc.BadJWTException;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
public class JwtValidator {

    public static void validateAndExtract(String jwtToken) throws ParseException, JOSEException, BadJOSEException {
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
        if (!jwsObject.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(verifier))) {
            throw new BadJWTException("Invalid signature");
        }

        // Extract the payload
        String payload = jwsObject.getPayload().toString();

        // Print header and payload for demonstration
        System.out.println("Header: " + jwsObject.getHeader().toJSONObject());
        System.out.println("Payload: " + payload);
    }
}
