package com.adorsys.webank.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
@Slf4j
@Component

public class CustomJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final com.adorsys.webank.config.CertValidator certValidator;

    @Autowired
    public CustomJwtAuthenticationConverter(com.adorsys.webank.config.CertValidator certValidator) {
        this.certValidator = certValidator;
    }

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

        Object accountJwt = headers.get("accountJwt");
        if (accountJwt != null) {
            log.debug("Found accountJwt in header of type: {}", accountJwt.getClass().getName());
            log.debug("AccountJwt value: {}", accountJwt);

            // Validate certificate before granting role
            try {
                boolean certValid = certValidator.validateJWT(jwt.getTokenValue());
                if (certValid) {
                    authorities.add(new SimpleGrantedAuthority(com.adorsys.webank.domain.Role.ACCOUNT_CERTIFIED.getRoleName()));
                    log.info("Certificate validated successfully, granting {}", com.adorsys.webank.domain.Role.ACCOUNT_CERTIFIED);
                } else {
                    log.warn("Certificate validation failed, not granting {}", com.adorsys.webank.domain.Role.ACCOUNT_CERTIFIED);
                }
            } catch (Exception e) {
                log.error("Error validating certificate", e);
            }
        } else {
            log.debug("No accountJwt found in headers. Available headers: {}", headers.keySet());
        }

        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        Collection<? extends GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);
        log.debug("Default converted authorities: {}", defaultAuthorities);

        authorities.addAll(defaultAuthorities);
        return authorities;
    }
}
