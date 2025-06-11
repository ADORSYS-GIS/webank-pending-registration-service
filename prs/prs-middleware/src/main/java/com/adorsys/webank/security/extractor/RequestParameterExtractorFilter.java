package com.adorsys.webank.security.extractor;

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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import com.adorsys.webank.security.extractor.EndpointParameterMapper;


@Component
@RequiredArgsConstructor
public class RequestParameterExtractorFilter extends OncePerRequestFilter {
    private static final ThreadLocal<Map<String, String>> REQUEST_PARAMS = new ThreadLocal<>();
    private static final Logger log = LoggerFactory.getLogger(RequestParameterExtractorFilter.class);
    @Autowired
    private final EndpointParameterMapper endpointParameterMapper;
    /**
     * List of paths to skip for parameter extraction.
     * These paths are typically used for static resources or API documentation.
     */
    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/h2-console",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/swagger-ui/"
    );

    /**
     * Retrieves the required parameters for the given endpoint path.
     * If no parameters are found, it tries to match the path without a leading slash.
     *
     * @param fullPath The full request path.
     * @return A list of required parameter names.
     */
    private List<String> getRequiredParameters(String fullPath) {
        // Remove the leading slash if present
        String normalizedPath = fullPath.startsWith("/") ? fullPath.substring(1) : fullPath;

        List<String> requiredParams = endpointParameterMapper.getRequiredParameters(normalizedPath);
        if (requiredParams.isEmpty()) {
            requiredParams = endpointParameterMapper.getRequiredParameters(fullPath);
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


    private Map<String, String> extractPathVariableParameters(String fullPath, String pattern, List<String> requiredParams) {
        Map<String, String> result = new LinkedHashMap<>();

        // Extract variable names from a pattern: e.g., {userId}, {username}
        List<String> varNames = new ArrayList<>();
        Pattern varPattern = Pattern.compile("\\{([^/]+?)}");
        Matcher varMatcher = varPattern.matcher(pattern);

        while (varMatcher.find()) {
            varNames.add(varMatcher.group(1));
        }

        // Build regex and match
        String regex = pattern.replaceAll("\\{([^/]+?)}", "([^/]+)");
        Pattern pathPattern = Pattern.compile(regex);
        Matcher matcher = pathPattern.matcher(fullPath);

        if (matcher.matches() && !varNames.isEmpty()) {
            for (int i = 0; i < varNames.size(); i++) {
                String paramName = varNames.get(i);
                String paramValue = matcher.group(i + 1); // group 0 is full match

                if (requiredParams.contains(paramName)) {
                    result.put(paramName, paramValue);
                    log.info("Extracted path parameter {} = {}", paramName, paramValue);
                }
            }
        }

        return result;
    }

    /**
     * Extracts parameters from the request path
     *
     * @param request The HttpServletRequest containing the request path and query string.
     * @param requiredParams The list of required parameter names.
     * @return A map of extracted parameters with their values.
     */

    private Map<String, String> extractGetParameters(HttpServletRequest request, List<String> requiredParams) {
        String fullPath = request.getRequestURI();
        log.debug("Full request path: {}", fullPath);

        Map<String, String> orderedParams = new LinkedHashMap<>();

        String normalizedPath = fullPath.startsWith("/") ? fullPath.substring(1) : fullPath;

        for (Map.Entry<String, List<String>> entry : endpointParameterMapper.getEndpointParameters().entrySet()) {
            String pattern = entry.getKey();

            if (pattern.contains("{")) {
                log.debug("Trying to match pattern: {} against path: {}", pattern, normalizedPath);

                String regex = pattern.replaceAll("\\{[^/]+?}", "([^/]+)");

                if (normalizedPath.matches(regex)) {
                    Map<String, String> pathVariables = extractPathVariableParameters(normalizedPath, pattern, requiredParams);
                    orderedParams.putAll(pathVariables);
                    log.info("Matched pattern: {} -> Extracted variables: {}", pattern, pathVariables);
                    break;
                } else if (fullPath.matches(regex)) {
                    Map<String, String> pathVariables = extractPathVariableParameters(fullPath, pattern, requiredParams);
                    orderedParams.putAll(pathVariables);
                    break;
                }
            }
        }

        return orderedParams;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        String requestURI = request.getRequestURI();
        if (SKIP_PATHS.stream().anyMatch(requestURI::startsWith)) {
            log.debug("Skipping filter for: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            HttpServletRequest wrappedRequest = new CachingRequestBodyWrapper(request);
            Map<String, String> params = new HashMap<>();
            
            String fullPath = wrappedRequest.getRequestURI();

            List<String> requiredParams = getRequiredParameters(fullPath);
            log.info("Required parameters for endpoint {}: {}", fullPath, requiredParams);
            
            if (wrappedRequest.getMethod().equals("POST")) {
                params = extractPostParameters(wrappedRequest, requiredParams);
            } else if (wrappedRequest.getMethod().equals("GET")) {
                params = extractGetParameters(wrappedRequest, requiredParams);
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
