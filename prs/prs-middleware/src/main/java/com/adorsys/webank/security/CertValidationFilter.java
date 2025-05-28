package com.adorsys.webank.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@Component
public class CertValidationFilter extends OncePerRequestFilter {

    @Autowired
    private CertValidator certValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Collection<? extends GrantedAuthority> authorities = jwtAuth.getAuthorities();

            if (authorities.contains(new SimpleGrantedAuthority("ROLE_ACCOUNT_CERTIFIED"))) {

                String token = jwtAuth.getToken().getTokenValue();

                boolean certValid = certValidator.validateJWT(token);
                if (!certValid) {
                    log.error("Certificate validation failed for endpoint requiring cert");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid certificate");
                    return;
                }

                log.info("Certificate validation passed");
            }
        }
        filterChain.doFilter(request, response);
    }
}

