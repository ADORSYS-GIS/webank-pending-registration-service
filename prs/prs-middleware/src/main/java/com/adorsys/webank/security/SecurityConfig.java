package com.adorsys.webank.security;

import com.adorsys.webank.config.*;
import com.adorsys.webank.domain.Role;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.security.extractor.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.config.http.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.*;
import org.springframework.web.cors.*;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private RequestParameterExtractorFilter requestParameterExtractorFilter;
    @Autowired
    private CertValidator certValidator;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .formLogin(AbstractHttpConfigurer::disable)
                    .addFilterBefore(requestParameterExtractorFilter, UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/prs/dev/**").authenticated()
                            .requestMatchers("/api/prs/email-otp/**").hasAuthority(Role.ACCOUNT_CERTIFIED.getRoleName())
                            .requestMatchers("/api/prs/kyc/**").hasAuthority(Role.ACCOUNT_CERTIFIED.getRoleName())
                            .requestMatchers("/api/prs/otp/**").hasAuthority(Role.ACCOUNT_CERTIFIED.getRoleName())
                            .requestMatchers("/api/prs/recovery/**").hasAuthority(Role.ACCOUNT_CERTIFIED.getRoleName())
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt
                                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                            )
                    );


            /* * Configures security headers to protect against common web vulnerabilities.
             * This includes XSS protection, Content Security Policy (CSP), Referrer Policy, and X-Frame-Options.
             */
            http.headers(headers -> headers
                            .xssProtection(xss -> xss
                                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                            )

                           /* * The Content Security Policy (CSP) header helps prevent XSS attacks by controlling which resources can be loaded.
                            * The policy here is set to 'none' for all directives, which means no resources can be loaded.
                           */
                            .contentSecurityPolicy(csp -> csp
                                    .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; style-src 'self'; img-src 'self' data:; connect-src 'self'; font-src 'self'; frame-src 'none'; base-uri 'self'; form-action 'self'")
                            )


                            /* * The Referrer-Policy header controls how much referrer information is included with requests.
                             * The SAME_ORIGIN policy means that the referrer will only be sent for same-origin requests.
                             */
                            .referrerPolicy(referrer -> referrer
                                    .policy(ReferrerPolicy.SAME_ORIGIN)
                            )


                          /* * X-frame-options header is used to prevent clickjacking attacks by controlling whether the page can be displayed in a frame.
                           * The SAME ORIGIN option allows the page to be displayed in a frame only if the request comes from the same origin.
                           */
                            .frameOptions(FrameOptionsConfig::sameOrigin
                            )

                            /* * The Permissions-Policy header allows you to control which features and APIs can be used in the browser.
                             * This example disables geolocation, microphone, camera, and payment features.
                             */
                            .permissionsPolicy(policy -> policy
                                    .policy("geolocation=(), microphone=(), camera=()")
                            )
             );



            return http.build();
        } catch (Exception e) {
            throw new SecurityConfigurationException("Failed to build security filter chain", e);
        }
    }
    /**
     * Custom JWT authentication converter to extract authorities from the JWT token.
     * This can be extended to include roles or other claims as needed.
     *
     * @return CustomJwtAuthenticationConverter instance
     */
    @Bean
    public CustomJwtAuthenticationConverter jwtAuthenticationConverter() {
        return new CustomJwtAuthenticationConverter(certValidator);
    }
}