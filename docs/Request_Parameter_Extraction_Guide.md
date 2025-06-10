# Request Parameter Extraction Guide

## Overview

This comprehensive guide explains the request parameter extraction module in our Spring application, which is crucial for JWT validation and security. The module consists of several components that work together to ensure proper parameter handling and validation.

## Core Components

### 1. `EndpointParameterMapper`

#### Purpose
Maps API endpoint paths to their required parameters using a flexible pattern matching system.

#### Implementation Details

```java
@SuperBuilder
@RequiredArgsConstructor
public class EndpointParameterMapper {
    private final Map<String, List<String>> endpointParameters;

    public List<String> getRequiredParameters(String path) {
        // Try exact match first
        List<String> params = endpointParameters.get(path);
        if (params != null) {
            return params;
        }

        // Try pattern matching with dynamic placeholders
        for (Map.Entry<String, List<String>> entry : endpointParameters.entrySet()) {
            String pattern = entry.getKey();
            
            if (pattern.contains("{")) {
                // Replace {xxx} with regex groups
                String regex = pattern.replaceAll("\\{[^/]+?\\}", "([^"]+)");
                if (path.matches(regex)) {
                    return entry.getValue();
                }
            }
        }
        
        return Collections.emptyList();
    }

    public Map<String, List<String>> getEndpointParameters() {
        return Collections.unmodifiableMap(endpointParameters);
    }
}
```

#### Key Features
- Supports both exact path matching and pattern matching
- Uses regex for dynamic path variables
- Thread-safe implementation with immutable collections
- Provides read-only access to endpoint parameters

#### Example Configuration
```java
@Bean
public EndpointParameterMapper endpointParameterMapper() {
    Map<String, List<String>> ENDPOINT_PARAMETERS = new HashMap<>();
    
    // KYC endpoints
    ENDPOINT_PARAMETERS.put("api/prs/kyc/findById/{DocumentUniqueId}", List.of("DocumentUniqueId"));
    ENDPOINT_PARAMETERS.put("api/prs/kyc/info", Arrays.asList("idNumber", "expiryDate", "accountId"));
    ENDPOINT_PARAMETERS.put("api/prs/kyc/documents", Arrays.asList("frontId", "backId", "selfieId", "taxId", "accountId"));
    ENDPOINT_PARAMETERS.put("api/prs/kyc/record", List.of("accountId"));
    
    return EndpointParameterMapper.builder()
            .endpointParameters(Collections.unmodifiableMap(ENDPOINT_PARAMETERS))
            .build();
}
```

#### Pattern Matching Examples
- `/api/prs/kyc/findById/123` matches `api/prs/kyc/findById/{DocumentUniqueId}`
- `/api/prs/kyc/info` matches exact path
- Supports nested path variables: `api/prs/user/{userId}/profile/{profileId}`

#### Best Practices
- Use consistent path patterns
- Document parameter requirements
- Maintain parameter order
- Use unmodifiable collections for thread safety

---

## 1. `EndpointParameterMapper.java`

### Purpose

Maps API endpoint paths to lists of required parameters that must be present in the request.

### Structure

```java
public class EndpointParameterMapper {
    private final Map<String, List<String>> endpointParameters;

    public List<String> getRequiredParameters(String path) { ... }
    public Map<String, List<String>> getEndpointParameters() { ... }
}
```

### How It Works

- Maintains a map where:
  - Key = API path (e.g., `"api/prs/kyc/findById/{DocumentUniqueId}"`)
  - Value = List of required parameter names (e.g., `["DocumentUniqueId"]`)
- Supports both:
  - Exact matches (`"api/prs/kyc/info"`)
  - Dynamic path variables using `{variable}` syntax

### Example Mapping

```java
ENDPOINT_PARAMETERS.put("api/prs/kyc/findById/{DocumentUniqueId}", List.of("DocumentUniqueId"));
```

Means:
- The URL `/api/prs/kyc/findById/123` requires `DocumentUniqueId=123`.

---

### 2. `RequestParameterExtractorFilter`

#### Purpose
Spring filter that extracts parameters from HTTP requests and prepares them for JWT validation.

#### Implementation Details

```java
@Component
public class RequestParameterExtractorFilter extends OncePerRequestFilter {
    private static final ThreadLocal<Map<String, String>> REQUEST_PARAMS = new ThreadLocal<>();
    private static final Logger log = LoggerFactory.getLogger(RequestParameterExtractorFilter.class);
    
    @Autowired
    private final EndpointParameterMapper endpointParameterMapper;

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

    private List<String> getRequiredParameters(String fullPath) {
        // Remove leading slash if present
        String normalizedPath = fullPath.startsWith("/") ? fullPath.substring(1) : fullPath;
        
        List<String> requiredParams = endpointParameterMapper.getRequiredParameters(normalizedPath);
        if (requiredParams.isEmpty()) {
            requiredParams = endpointParameterMapper.getRequiredParameters(fullPath); // try original
        }
        return requiredParams;
    }

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

    private Map<String, String> extractGetParameters(HttpServletRequest request, List<String> requiredParams) {
        Map<String, String> orderedParams = new LinkedHashMap<>();
        
        String normalizedPath = request.getRequestURI().startsWith("/") ? 
            request.getRequestURI().substring(1) : request.getRequestURI();

        for (Map.Entry<String, List<String>> entry : endpointParameterMapper.getEndpointParameters().entrySet()) {
            String pattern = entry.getKey();
            
            if (pattern.contains("{")) {
                log.debug("Trying to match pattern: {} against path: {}", pattern, normalizedPath);
                
                String regex = pattern.replaceAll("\\{[^/]+?\\}", "([^"]+)");
                if (normalizedPath.matches(regex)) {
                    Map<String, String> pathVariables = extractPathVariableParameters(normalizedPath, pattern, requiredParams);
                    orderedParams.putAll(pathVariables);
                    log.info("Matched pattern: {} -> Extracted variables: {}", pattern, pathVariables);
                    break;
                } else if (request.getRequestURI().matches(regex)) {
                    Map<String, String> pathVariables = extractPathVariableParameters(request.getRequestURI(), pattern, requiredParams);
                    orderedParams.putAll(pathVariables);
                    break;
                }
            }
        }
        
        return orderedParams;
    }

    private Map<String, String> extractPathVariableParameters(String fullPath, String pattern, List<String> requiredParams) {
        Map<String, String> result = new LinkedHashMap<>();
        
        // Extract variable names from pattern
        Pattern varPattern = Pattern.compile("\\{([^/]+?)\\}");
        Matcher varMatcher = varPattern.matcher(pattern);
        List<String> varNames = new ArrayList<>();
        
        while (varMatcher.find()) {
            varNames.add(varMatcher.group(1));
        }
        
        // Build regex and match
        String regex = pattern.replaceAll("\\{([^/]+?)\\}", "([^"]+)");
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

    public static Map<String, String> getCurrentRequestParams() {
        return REQUEST_PARAMS.get();
    }
}
```

#### Key Features
1. **Parameter Extraction Methods**:
   - `extractGetParameters`: Handles GET requests with path variables
   - `extractPostParameters`: Handles POST requests with JSON bodies
   - `extractPathVariableParameters`: Extracts values from path variables

2. **Path Variable Handling**:
   - Uses regex to extract variable names from patterns
   - Supports nested path variables
   - Maintains parameter order
   - Handles both normalized and original paths

3. **ThreadLocal Storage**:
   - Stores parameters per request
   - Thread-safe implementation
   - Automatically cleaned up after request
   - Provides access via static method

#### Best Practices
- Always log parameter extraction
- Handle missing parameters gracefully
- Maintain parameter order
- Use appropriate logging levels
- Clean up ThreadLocal storage

### Purpose

Spring filter that runs once per request. It:
- Identifies what parameters are required for the current endpoint.
- Extracts them from:
  - **GET requests**: via dynamic path segments.
  - **POST requests**: via JSON body.
- Stores them in a thread-local variable for downstream use.

###  Structure

```java
@Component
public class RequestParameterExtractorFilter extends OncePerRequestFilter {
    ...
    
    @Override
    protected void doFilterInternal(...) { ... }

    private List<String> getRequiredParameters(String fullPath) { ... }
    private Map<String, String> extractGetParameters(...) { ... }
    private Map<String, String> extractPostParameters(...) { ... }
    private Map<String, String> extractPathVariableParameters(...) { ... }
}
```

### Key Methods

| Method | Description |
|-------|-------------|
| `getRequiredParameters(path)` | Finds which parameters are needed for the current endpoint. Normalizes path before matching. |
| `extractGetParameters(...)` | Extracts path variables like `DocumentUniqueId` from URLs such as `/findById/123`. |
| `extractPostParameters(...)` | Extracts parameters from POST request bodies (JSON). |
| `extractPathVariableParameters(...)` | Uses regex capture groups to dynamically extract values from path variables. |

### Thread Local Storage

#### Purpose
Stores extracted parameters per request in a thread-safe manner.

#### Implementation
```java
private static final ThreadLocal<Map<String, String>> REQUEST_PARAMS = new ThreadLocal<>();

// Accessing parameters:
public static Map<String, String> getCurrentRequestParams() {
    return REQUEST_PARAMS.get();
}
```

#### Key Features
- Thread-safe storage
- Automatic cleanup
- Request-scoped storage
- Static access
- Immutable collections

#### Best Practices
- Always clean up after use
- Use appropriate collection types
- Handle null cases
- Document access patterns

---

## Integration with JWT Validation

The extracted parameters are crucial for JWT validation:

1. **Parameter Order**:
   - Parameters must be in the order specified in `EndpointConfig`
   - Order is crucial for payload hash validation
   - Maintained through `LinkedHashMap`

2. **Hash Generation**:
```java
// In JwtValidator
public void validatePayloadHash(JWSObject jwsObject, List<String> parameters) {
    String payload = jwsObject.getPayload().toString();
    String hash = DigestUtils.sha256Hex(String.join("", parameters));
    
    if (!payload.equals(hash)) {
        throw new BadJWTException("Invalid payload hash");
    }
}
```

3. **Validation Flow**:
   - Extract parameters from request
   - Generate hash from parameters
   - Compare with JWT payload
   - Validate signature
   - Verify claims

---

## Common Issues and Solutions

1. **Missing Parameters**
   - Verify configuration in `EndpointConfig`
   - Check path variable syntax
   - Ensure parameter order matches
   - Add proper error handling

2. **Pattern Matching**
   - Use exact paths
   - Correct path variable syntax
   - Avoid trailing slashes
   - Test with different inputs

3. **JWT Validation**
   - Verify parameter order
   - Check for spaces
   - Confirm hash algorithm
   - Validate all parameters

4. **Memory Issues**
   - Clean up ThreadLocal
   - Handle large payloads
   - Use appropriate collections
   - Monitor memory usage

---

## Debugging Tips

1. Enable DEBUG logging:
```properties
logging.level.com.adorsys.webank.security=DEBUG
```

2. Check logs for:
   - Parameter extraction
   - Pattern matching
   - JWT validation
   - Request processing
   - Error handling

3. Use curl with verbose mode:
```bash
curl -v http://localhost:8080/api/prs/kyc/findById/44
```

4. Test with different scenarios:
   - Valid requests
   - Missing parameters
   - Invalid paths
   - Large payloads
   - Concurrent requests

---

## Best Practices

1. **Endpoint Configuration**
   - Use consistent patterns
   - Document requirements
   - Maintain parameter order
   - Use unmodifiable collections

2. **Error Handling**
   - Proper error messages
   - Graceful failures
   - Comprehensive logging
   - Input validation

3. **Security**
   - Validate all parameters
   - Secure hash algorithms
   - Proper request validation
   - Thread safety

4. **Performance**
   - Efficient collections
   - Memory management
   - ThreadLocal cleanup
   - Resource handling

---

## Maintenance Checklist

| Task | Description |
|------|-------------|
| Endpoint Configuration | Verify all endpoints are properly mapped with required parameters |
| Pattern Matching | Test all path variable patterns for correctness |
| Parameter Extraction | Ensure all required parameters are extracted correctly |
| JWT Validation | Confirm payload hash generation and validation work as expected |
| Logging | Check that all important operations are properly logged |
| Error Handling | Verify all error cases are handled appropriately |
| Performance | Monitor memory usage and request processing times |
| Security | Review parameter validation and access control |
| Testing | Regularly test with different scenarios and edge cases |

---

### 3. `CachingRequestBodyWrapper`

#### Purpose
Allows multiple reads of the HTTP request body, which is essential for POST requests since the original `HttpServletRequest` only allows one read.

#### Implementation Details
```java
public class CachingRequestBodyWrapper extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    public CachingRequestBodyWrapper(HttpServletRequest request) throws IOException {
        super(request);
        cachedBody = IOUtils.toByteArray(request.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}
```

#### Key Features
- Caches the request body in memory
- Implements `ServletInputStream` for compatibility
- Supports multiple reads
- Thread-safe implementation
- Handles both text and binary data

#### Best Practices
- Use only when necessary (for POST requests)
- Be mindful of memory usage with large payloads
- Properly handle exceptions
- Clean up resources

### Purpose

Allows multiple reads of the HTTP request body in filters — something not supported natively by `HttpServletRequest`.

> Required so we can read and re-read the body in filters and controllers.


---

## Developer Guidelines

### When Adding New Endpoints

Update `EndpointConfig.java`:

```java
@Bean
public EndpointParameterMapper endpointParameterMapper() {
    Map<String, List<String>> ENDPOINT_PARAMETERS = new HashMap<>();

    ENDPOINT_PARAMETERS.put("api/prs/kyc/findById/{DocumentUniqueId}", List.of("DocumentUniqueId"));

    // Add new routes here
    ENDPOINT_PARAMETERS.put("api/prs/user/{userId}/profile", List.of("userId"));

    return EndpointParameterMapper.builder()
            .endpointParameters(Collections.unmodifiableMap(ENDPOINT_PARAMETERS))
            .build();
}
```

### When Extending Extraction Logic

If you want to support query parameters too:

```java
private Map<String, String> extractQueryParameters(HttpServletRequest request, List<String> requiredParams) {
    Map<String, String> result = new LinkedHashMap<>();
    for (String paramName : requiredParams) {
        String value = request.getParameter(paramName);
        if (value != null) {
            result.put(paramName, value);
        }
    }
    return result;
}
```

Then merge results:

```java
params.putAll(extractQueryParameters(request, requiredParams));
```

---

## Testing Tips

Use tools like Postman or curl:

### GET Example

```bash
curl -X GET http://localhost:8080/api/prs/kyc/findById/67
```

Expected log output:

```
INFO  Required parameters for endpoint /api/prs/kyc/findById/67: [DocumentUniqueId]
INFO  Matched pattern: api/prs/kyc/findById/{DocumentUniqueId} -> Extracted variables: {DocumentUniqueId=67}
INFO  Extracted parameters for path /api/prs/kyc/findById/67: {DocumentUniqueId=67}
```

### POST Example

```json
{
  "token": "abc123",
  "accountId": "user456"
}
```

Expected log output:

```
INFO  Extracted parameter token with value: abc123
INFO  Extracted parameter accountId with value: user456
INFO  Extracted parameters for path /api/prs/email-otp/validate: {token=abc123, accountId=user456}
```

---

##  Maintenance Checklist

| Task | Done? |
|------|-------|
| Ensure `CachingRequestBodyWrapper` exists |  |
| Update `EndpointConfig` when adding new routes |  |
| Normalize paths before matching in `getRequiredParameters(...)` | |
| Use regex capture groups for dynamic path extraction |  |
| Log matched patterns and extracted variables |  |
| Consider query param support if needed | |

---

## Summary

| Component | Responsibility |
|----------|----------------|
| `EndpointParameterMapper` | Maps endpoints to required parameters |
| `RequestParameterExtractorFilter` | Extracts parameters at runtime |
| `CachingRequestBodyWrapper` | Allows reading POST body multiple times |
| ThreadLocal Storage | Stores extracted data per request |

