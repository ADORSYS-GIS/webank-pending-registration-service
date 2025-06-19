package com.adorsys.webank.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class JwtUtils {
    
    private JwtUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static JWSHeader createJwtHeader(JWK publicKey) {
        try {
            String kid = generateKidFromPublicKey(publicKey);
            return new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(kid)
                    .type(JOSEObjectType.JWT)
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to create JWT header", e);
        }
    }
    
    public static String generateKidFromPublicKey(JWK publicKey) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(publicKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

}
