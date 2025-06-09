package com.adorsys.webank.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds a correlation ID to the Mapped Diagnostic Context (MDC)
 * for each request. If the request already has a correlation ID header,
 * it will use that, otherwise it will generate a new one.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
  
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Check if the request has a correlation ID header, otherwise generate a new one
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = generateCorrelationId();
            }

            // Add the correlation ID to the MDC
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add request path and method to MDC for additional context
            MDC.put("requestPath", request.getRequestURI());
            MDC.put("requestMethod", request.getMethod());

            // Set the correlation ID in the response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Always clear the MDC to prevent memory leaks
            MDC.clear();
        }
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
} 