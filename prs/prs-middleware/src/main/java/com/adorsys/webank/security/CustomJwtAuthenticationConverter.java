package com.adorsys.webank.security;

import com.adorsys.webank.config.CertValidator;
import com.adorsys.webank.domain.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import com.adorsys.webank.exceptions.SecurityConfigurationException;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final CertValidator certValidator;

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        log.info("Converting JWT to JwtAuthenticationToken");
        log.debug("JWT headers: {}", jwt.getHeaders());
        log.debug("JWT claims: {}", jwt.getClaims());

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        log.debug("Extracted authorities: {}", authorities);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Map<String, Object> headers = jwt.getHeaders();

        log.info("Extracting authorities from JWT headers");
        log.debug("Processing headers: {}", headers);

        try {
            boolean certValid = certValidator.validateJWT(jwt.getTokenValue());
            if (!certValid) {
                log.warn("Certificate validation failed, no roles granted.");
                return authorities;
            }

            // Check for accountJwt
            if (headers.containsKey("accountJwt")) {
                authorities.add(new SimpleGrantedAuthority(Role.ACCOUNT_CERTIFIED.getRoleName()));
                log.info("Granted {}", Role.ACCOUNT_CERTIFIED);
            }

            // Check for kycCertJwt
            if (headers.containsKey("kycCertJwt")) {
                authorities.add(new SimpleGrantedAuthority(Role.KYC_CERT.getRoleName()));
                log.info("Granted {}", Role.KYC_CERT);
            }

            // Check for kycJwt
            if (headers.containsKey("kycJwt")) {
                authorities.add(new SimpleGrantedAuthority(Role.KYC_CERT.getRoleName()));
                log.info("Granted {}", Role.KYC_CERT);
            }

            // Check for devJwt
            if (headers.containsKey("devJwt")) {
                authorities.add(new SimpleGrantedAuthority(Role.DEVICE_CERT.getRoleName()));
                log.info("Granted {}", Role.DEVICE_CERT);

            }
        } catch (Exception e) {
            log.error("Error validating certificate", e);
        }

        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        Collection<? extends GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);
        log.debug("Default converted authorities: {}", defaultAuthorities);

        authorities.addAll(defaultAuthorities);
        return authorities;
    }
}