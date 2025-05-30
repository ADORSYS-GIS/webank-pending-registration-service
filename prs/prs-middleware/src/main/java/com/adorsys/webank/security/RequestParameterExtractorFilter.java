package com.adorsys.webank.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import  jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class RequestParameterExtractorFilter extends OncePerRequestFilter {
    private static final ThreadLocal<Map<String, String>> REQUEST_PARAMS = new ThreadLocal<>();
    private static final Logger log = LoggerFactory.getLogger(RequestParameterExtractorFilter.class);
    @Autowired
    private final EndpointParameterMapper endpointParameterMapper;

    public RequestParameterExtractorFilter(EndpointParameterMapper endpointParameterMapper) {
        this.endpointParameterMapper = endpointParameterMapper;
    }

    /**
     * Retrieves the required parameters for the given endpoint path.
     * If no parameters are found, it tries to match the path without a leading slash.
     *
     * @param fullPath The full request path.
     * @return A list of required parameter names.
     */
    private List<String> getRequiredParameters(String fullPath) {
        List<String> requiredParams = endpointParameterMapper.getRequiredParameters(fullPath);
        if (requiredParams.isEmpty()) {
            // Try to match without leading slash
            String pathWithoutLeadingSlash = fullPath.startsWith("/") ? fullPath.substring(1) : fullPath;
            requiredParams = endpointParameterMapper.getRequiredParameters(pathWithoutLeadingSlash);
        }
        return requiredParams;
    }
    /**
     * Extracts parameters from the request body for POST requests.
     *
     * @param request The HttpServletRequest containing the request body.
     * @param requiredParams The list of required parameter names.
     * @return A map of extracted parameters with their values.
     * @throws IOException If an error occurs while reading the request body.
     */

    private Map<String, String> extractPostParameters(HttpServletRequest request, List<String> requiredParams) throws IOException {
        String requestBody = request.getReader().lines().collect(Collectors.joining());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestBody);
        log.debug("Request body JSON: {}", jsonNode);
        
        Map<String, String> orderedParams = new LinkedHashMap<>();
        for (String paramName : requiredParams) {
            JsonNode paramNode = jsonNode.get(paramName);
            if (paramNode != null) {
                String paramValue = paramNode.asText();
                orderedParams.put(paramName, paramValue);
                log.info("Extracted parameter {} with value: {}", paramName, paramValue);
            } else {
                log.warn("Parameter {} not found in request body", paramName);
            }
        }
        return orderedParams;
    }

    /**
     * Extracts parameters from the request path for GET requests.
     *
     * @param fullPath The full request path.
     * @param requiredParams The list of required parameter names.
     * @return A map of extracted parameters with their values.
     */
    private Map<String, String> extractGetParameters(String fullPath, List<String> requiredParams) {
        String[] pathSegments = fullPath.split("/");
        log.debug("Path segments: {}", Arrays.toString(pathSegments));
        
        Map<String, String> orderedParams = new LinkedHashMap<>();
        for (String paramName : requiredParams) {
            for (String segment : pathSegments) {
                if (segment.startsWith(paramName)) {
                    String paramValue = segment.replace(paramName + "/", "");
                    orderedParams.put(paramName, paramValue);
                    log.info("Extracted path parameter {} with value: {}", paramName, paramValue);
                    break;
                }
            }
        }
        return orderedParams;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            HttpServletRequest wrappedRequest = new CachingRequestBodyWrapper(request);
            Map<String, String> params = new HashMap<>();
            
            String fullPath = wrappedRequest.getRequestURI();
            log.debug("Full request path: {}", fullPath);
            
            List<String> requiredParams = getRequiredParameters(fullPath);
            log.info("Required parameters for endpoint {}: {}", fullPath, requiredParams);
            
            if (wrappedRequest.getMethod().equals("POST")) {
                params = extractPostParameters(wrappedRequest, requiredParams);
            } else if (wrappedRequest.getMethod().equals("GET")) {
                params = extractGetParameters(fullPath, requiredParams);
            }
            
            log.info("Extracted parameters for path {}: {}", fullPath, params);
            log.debug("Final extracted parameters: {}", params);
            
            REQUEST_PARAMS.set(params);
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            REQUEST_PARAMS.remove();
        }
    }

    public static Map<String, String> getCurrentRequestParams() {
        return REQUEST_PARAMS.get();
    }
}
