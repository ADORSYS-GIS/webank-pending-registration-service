package com.adorsys.webank.security;

import com.adorsys.webank.exceptions.SecurityConfigurationException;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import com.adorsys.webank.domain.Role;
import com.adorsys.webank.config.CertValidator;
import org.springframework.security.config.http.SessionCreationPolicy;
import com.adorsys.webank.security.headers.SecurityHeadersConfig;
import com.adorsys.webank.security.extractor.RequestParameterExtractorFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private RequestParameterExtractorFilter requestParameterExtractorFilter;
    @Autowired
    private CertValidator certValidator;
    @Autowired
    private SecurityHeadersConfig securityHeadersConfig;


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

            securityHeadersConfig.configureHeaders(http);

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