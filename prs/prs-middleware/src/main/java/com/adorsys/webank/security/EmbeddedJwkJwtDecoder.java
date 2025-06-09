package com.adorsys.webank.security;

import com.nimbusds.jwt.*;
import com.nimbusds.jose.util.JSONObjectUtils;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.*;
import java.time.Instant;
import java.util.*;
import com.adorsys.webank.exceptions.SecurityConfigurationException;
import com.adorsys.webank.exceptions.JwtPayloadParseException;

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
            throw new SecurityConfigurationException("JWT token is null or empty", null);
        }
        
        try {
            // Get request parameters from ThreadLocal
            Map<String, String> requestParams = com.adorsys.webank.security.extractor.RequestParameterExtractorFilter.getCurrentRequestParams();
            String[] params = requestParams.values().toArray(new String[0]);
            
            log.info("Validating JWT using JwtValidator with params: {}", Arrays.toString(params));
            com.adorsys.webank.config.JwtValidator.validateAndExtract(token, params);
            log.info("JWT validated successfully");

            SignedJWT signedJWT = SignedJWT.parse(token);
            log.debug("Parsed SignedJWT: {}", signedJWT);

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            log.debug("Extracted JWTClaimsSet: {}", claimsSet);

            Instant expiresAt = claimsSet.getExpirationTime() != null ?
                    claimsSet.getExpirationTime().toInstant() : null;
            log.debug("Token expiration time: {}", expiresAt);

            Instant issuedAt = claimsSet.getIssueTime() != null ?
                    claimsSet.getIssueTime().toInstant() : null;
            log.debug("Token issued time: {}", issuedAt);

            String subject = claimsSet.getSubject();
            log.debug("Token subject: {}", subject);

            Map<String, Object> headers = signedJWT.getHeader().toJSONObject();
            log.debug("JWT headers: {}", headers);

            Map<String, Object> claims = claimsSet.getClaims();
            log.debug("JWT claims: {}", claims);

            log.info("Returning validated Jwt object");
            return new Jwt(
                    token,
                    issuedAt,
                    expiresAt,
                    headers,
                    claims
            );

        } catch (java.text.ParseException e) {
            log.error("Failed to parse JWT", e);
            throw new JwtPayloadParseException("Failed to parse JWT: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("JWT validation failed", e);
            throw new SecurityConfigurationException("Invalid JWT: " + e.getMessage(), e);
        }
    }
}
