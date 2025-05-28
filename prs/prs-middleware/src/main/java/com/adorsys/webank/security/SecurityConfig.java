package com.adorsys.webank.security;


import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.web.*;
import org.springframework.web.cors.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/prs/dev/**").authenticated()
                        .requestMatchers("/api/prs/email-otp/**").hasAuthority("ROLE_ACCOUNT_CERTIFIED")
                        .requestMatchers("/api/prs/kyc/**").hasAuthority("ROLE_ACCOUNT_CERTIFIED")
                        .requestMatchers("/api/prs/otp/**").hasAuthority("ROLE_ACCOUNT_CERTIFIED")
                        .requestMatchers("/api/prs/recovery/**").hasAuthority("ROLE_ACCOUNT_CERTIFIED")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5173");
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
        return new CustomJwtAuthenticationConverter();
    }
}

