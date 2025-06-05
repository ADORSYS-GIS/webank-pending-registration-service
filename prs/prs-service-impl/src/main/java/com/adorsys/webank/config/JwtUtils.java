package com.adorsys.webank.config;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

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
