package com.adorsys.webank.config;

import lombok.extern.slf4j.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.oauth2.jwt.*;
import com.nimbusds.jose.jwk.ECKey;

import java.util.*;

/**
 * Utility class for working with Spring Security's context.
 * Provides methods to access the current user's JWT, authentication status, and authorities.
 */
@Slf4j
public class SecurityUtils {

    /**
     * Retrieves the current user's JWT token if available.
     *
     * @return an Optional containing the JWT token string if present, otherwise an empty Optional.
     */
    public static Optional<String> getCurrentUserJWT() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            log.debug("JWT token found for current user");
            log.info("jwt token from current spint context is {}", jwt.getTokenValue());
            return Optional.ofNullable(jwt.getTokenValue());
        }
        log.debug("No JWT token found for current user");
        return Optional.empty();
    }

    /**
     * Convenient helper method to extract device JWK from current user's JWT in security context.
     * Combines getting the current JWT from SecurityUtils and extracting the device JWK.
     *
     * @return The device public key as JSON string
     * @throws IllegalArgumentException if no JWT found in context or extraction fails
     */
    public static ECKey extractDeviceJwkFromContext() {
        return getCurrentUserJWT()
                .map(JwtValidator::extractDeviceJwk)
                .orElseThrow(() -> new IllegalArgumentException("No JWT found in security context"));

    }

}
