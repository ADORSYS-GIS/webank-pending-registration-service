package com.adorsys.webank.security;

import com.adorsys.webank.config.*;
import com.adorsys.webank.domain.Role;
import com.adorsys.webank.exceptions.SecurityConfigurationException;
import com.adorsys.webank.security.extractor.RequestParameterExtractorFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .formLogin(AbstractHttpConfigurer::disable)
                    .addFilterBefore(requestParameterExtractorFilter, UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/prs/dev/**").authenticated()
                            .requestMatchers("/h2-console/**").permitAll()
                            .requestMatchers("/swagger-ui.html/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
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


                           /* * Hsts (HTTP Strict Transport Security) header is used to enforce secure connections to the server.
                            * It tells the browser to only connect to the server using HTTPS for a specified period.
                           */

                           .httpStrictTransportSecurity(hsts -> hsts
                                  .includeSubDomains(true)
                                  .preload(true)
                                  .maxAgeInSeconds(31536000)
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
                             * This example disables geolocation, microphone, camera,
                             */
                            .permissionsPolicy(policy -> policy
                                    .policy("geolocation=(), microphone=(), camera=(self)")
                            )
             );



            return http.build();
        } catch (Exception e) {
            throw new SecurityConfigurationException("Failed to build security filter chain", e);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:8080");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return source;
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