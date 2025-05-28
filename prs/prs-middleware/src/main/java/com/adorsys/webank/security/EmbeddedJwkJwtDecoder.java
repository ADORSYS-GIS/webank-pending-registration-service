package com.adorsys.webank.security;

import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.*;

/**
 * Uses JwtValidator to validate JWTs, and returns Spring Security's Jwt object.
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class EmbeddedJwkJwtDecoder implements JwtDecoder {


    @Override
    public Jwt decode(String token) throws JwtException {
        log.info("Starting decode process for JWT");
        log.debug("Incoming token: {}", token);

        if (token == null || token.isEmpty()) {
            log.error("JWT token is null or empty");
            throw new org.springframework.security.oauth2.jwt.BadJwtException("JWT token is null or empty");
        }
        log.info("token is {}", token);
        try {
            log.info("Validating JWT using JwtValidator");
            com.adorsys.webank.security.JwtValidator.validateAndExtract(token);
            log.info("JWT validated successfully");

            com.nimbusds.jwt.SignedJWT signedJWT = com.nimbusds.jwt.SignedJWT.parse(token);
            log.debug("Parsed SignedJWT: {}", signedJWT);

            com.nimbusds.jwt.JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            log.debug("Extracted JWTClaimsSet: {}", claimsSet);

            java.time.Instant expiresAt = claimsSet.getExpirationTime() != null ?
                    claimsSet.getExpirationTime().toInstant() : null;
            log.debug("Token expiration time: {}", expiresAt);

            java.time.Instant issuedAt = claimsSet.getIssueTime() != null ?
                    claimsSet.getIssueTime().toInstant() : null;
            log.debug("Token issued time: {}", issuedAt);

            String subject = claimsSet.getSubject();
            log.debug("Token subject: {}", subject);

            java.util.Map<String, Object> headers = signedJWT.getHeader().toJSONObject();
            log.debug("JWT headers: {}", headers);

            java.util.Map<String, Object> claims = claimsSet.getClaims();
            log.debug("JWT claims: {}", claims);

            log.info("Returning validated Jwt object");
            return new org.springframework.security.oauth2.jwt.Jwt(
                    token,
                    issuedAt,
                    expiresAt,
                    headers,
                    claims
            );

        } catch (Exception e) {
            log.error("JWT validation failed", e);
            throw new org.springframework.security.oauth2.jwt.BadJwtException("Invalid JWT", e);
        }
    }
        }
