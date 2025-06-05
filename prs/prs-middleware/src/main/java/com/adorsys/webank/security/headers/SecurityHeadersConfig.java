package com.adorsys.webank.security.headers;

import com.adorsys.webank.properties.*;
import lombok.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.*;
import org.springframework.security.web.header.writers.*;

/**
 * Security configuration class that sets up various HTTP security headers to protect against common web vulnerabilities.
 * This configuration is disabled by default and can be enabled through application properties.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityHeadersConfig {

    private final SecurityHeadersProperties securityHeaders;

    public void configureHeaders(HttpSecurity http) throws Exception {
        if (!securityHeaders.isEnabled()) {
            return;
        }

        http.headers(headers -> {
            // Content Security Policy (CSP)
            if (securityHeaders.getCsp().getPolicyDirectives() != null) {
                headers.contentSecurityPolicy(csp -> csp
                        .policyDirectives(securityHeaders.getCsp().getPolicyDirectives()));
            }

            // X-XSS-Protection
            if (securityHeaders.getXXssProtection() != null) {
                headers.xssProtection(xss -> xss
                        .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
            }

            // X-Frame-Options
            if (securityHeaders.getXFrameOptions() != null) {
                headers.frameOptions(FrameOptionsConfig::sameOrigin);
            }

            // Referrer-Policy
            if (securityHeaders.getReferrerPolicy() != null) {
                headers.addHeaderWriter(new StaticHeadersWriter("Referrer-Policy",
                        securityHeaders.getReferrerPolicy()));
            }

            // Permissions-Policy
            if (securityHeaders.getPermissionsPolicy() != null) {
                headers.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                        securityHeaders.getPermissionsPolicy()));
            }

            // HTTP Strict Transport Security (HSTS)
            headers.httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(securityHeaders.getHsts().getMaxAgeInSeconds())
                    .includeSubDomains(securityHeaders.getHsts().isIncludeSubDomains()));
        });

    }
}