package com.adorsys.webank.config;

import com.adorsys.webank.security.JwtValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // Validate and extract JWK (and perform all custom validation)
            JwtValidator.validateAndExtract(token);

            // Parse JWT claims (Spring expects claims in the payload, not header)
            com.nimbusds.jwt.SignedJWT signedJWT = com.nimbusds.jwt.SignedJWT.parse(token);
            Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
            Map<String, Object> headers = signedJWT.getHeader().toJSONObject();

            // Set issuedAt and expiresAt if present
            Instant issuedAt = claims.containsKey("iat") ? Instant.ofEpochSecond(((Number) claims.get("iat")).longValue()) : null;
            Instant expiresAt = claims.containsKey("exp") ? Instant.ofEpochSecond(((Number) claims.get("exp")).longValue()) : null;

            return new Jwt(
                token,
                issuedAt,
                expiresAt,
                headers,
                claims
            );
        } catch (ParseException | JOSEException | BadJOSEException | NoSuchAlgorithmException | JsonProcessingException e) {
            throw new JwtValidationException("Invalid JWT: " + e.getMessage(), Collections.emptyList());
        }
    }
} 