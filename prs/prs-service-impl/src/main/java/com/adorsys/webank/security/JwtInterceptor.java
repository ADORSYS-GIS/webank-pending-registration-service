package com.adorsys.webank.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtValidator jwtValidator;
    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);

    public JwtInterceptor(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Log the incoming request for debugging
        logger.info("Intercepting request: {} {}", request.getMethod(), request.getRequestURI());

        // Allow OPTIONS requests to pass through (preflight requests)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            logger.debug("Allowing OPTIONS request for preflight check");
            return true;
        }

        // Extract the Authorization header
        String authorizationHeader = request.getHeader("Authorization");

        // Check if the header is missing or doesn't start with "Bearer "
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logger.warn("Authorization header is missing or invalid");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authorization header must start with 'Bearer '");
            return false;
        }

        // Extract the JWT token (remove "Bearer " prefix)
        String jwtToken = authorizationHeader.substring(7);
        logger.debug("Extracted JWT token: {}", jwtToken);
        try {

            // Pass the JWT and the string representation of the request body to validateAndExtract
            JWK publicKey = jwtValidator.validateAndExtract(jwtToken);

            logger.info("JWT token validated successfully");

            // If validation succeeds, allow the request to proceed
            return true;
        } catch (Exception e) {
            // Log the validation error
            logger.error("JWT validation failed: {}", e.getMessage(), e);

            // Handle validation errors
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid JWT: " + e.getMessage());
            return false;
        }
    }
}