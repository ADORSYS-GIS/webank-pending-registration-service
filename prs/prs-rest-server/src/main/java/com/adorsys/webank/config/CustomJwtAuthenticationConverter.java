package com.adorsys.webank.config;

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

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {
    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Map<String, Object> headers = jwt.getHeaders();

        // Map custom header claims to roles
        if (Boolean.TRUE.equals(headers.get("deviceCertified"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_DEVICE_CERTIFIED"));
        }
        if (Boolean.TRUE.equals(headers.get("accountCertified"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ACCOUNT_CERTIFIED"));
        }
        if (Boolean.TRUE.equals(headers.get("kycCertified"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_KYC_CERTIFIED"));
        }

        // Optionally, add authorities from a standard claim (like 'scope' or 'authorities')
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        authorities.addAll(defaultConverter.convert(jwt));

        return authorities;
    }
} 