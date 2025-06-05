
#  Webank Security Architecture Documentation

## Introduction

This document outlines the security components implemented in the `webank` project under the packages:

- `com.adorsys.webank.security`
- `com.adorsys.webank.config`

These components handle JWT validation, certificate validation, and Spring Security integration to secure REST endpoints such as email OTP, KYC, and device registration. The goal is to ensure JWT authenticity, integrity, and payload consistency while leveraging Spring Security for robust authentication and authorization.

This guide is designed for new developers joining the team, offering a clear understanding of each component's purpose, functionality, and interactions.

## Architecture Overview

The Webank Security Architecture is now organized into two main modules to enhance reusability and maintainability:

### 1. Core Security Module (`prs-middleware`)

This module contains the core security components that can be reused across different applications:

- `JwtValidator`: Validates JWT tokens, signatures, and payload hashes
- `EmbeddedJwkJwtDecoder`: Integrates JWT validation with Spring Security
- `CertValidator`: Validates certificates embedded in JWT headers
- `RequestParameterExtractorFilter`: Extracts and validates request parameters
- `CachingRequestBodyWrapper`: Handles request body caching for multiple reads
- `CustomJwtAuthenticationConverter`: Handles JWT authentication and authorization

These components are designed to be generic and work with any endpoint configuration, making them highly reusable across different applications.

### 2. Endpoint Configuration Module (`prs-rest-server`)

This module contains the endpoint-specific configurations:

- `EndpointConfig`: Defines endpoint-parameter mappings that can be extended or overridden by other applications
- `EndpointParameterMapper`: Maps endpoints to their required parameters and validation rules

### Separation Benefits

1. **Reusability**
   - Core security components can be used by any application without modification
   - Each application can define its own endpoints while using the common security infrastructure
   - Security updates in core components are automatically available to all applications

2. **Flexibility**
   - Applications can extend or override endpoint configurations as needed
   - Custom security requirements can be implemented while maintaining core functionality
   - Each application can define its own security rules and validation logic

3. **Maintainability**
   - Security updates can be made in one place and propagated to all applications
   - Common security issues can be fixed centrally
   - Each module has a clear responsibility and scope

### Use Cases for Future Development

#### 1. Creating a New Application

When creating a new application that needs Webank's security features:

1. Add Dependencies
```xml
<dependency>
    <groupId>com.adorsys.webank</groupId>
    <artifactId>prs-middleware</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>com.adorsys.webank</groupId>
    <artifactId>prs-rest-server</artifactId>
    <version>0.0.1</version>
</dependency>
```

2. Create Custom Endpoint Configuration
```java
@Configuration
public class CustomEndpointConfig extends EndpointConfig {
    @Override
    @Bean
    public EndpointParameterMapper endpointParameterMapper() {
        final Map<String, List<String>> ENDPOINT_PARAMETERS = new HashMap<>();
        
        // Add custom endpoints
        ENDPOINT_PARAMETERS.put("/api/custom/endpoint1", List.of("param1", "param2"));
        
        // Optionally use base configuration
        ENDPOINT_PARAMETERS.putAll(super.endpointParameterMapper().getEndpointParameters());
        
        return EndpointParameterMapper.builder()
            .endpointParameters(ENDPOINT_PARAMETERS)
            .build();
    }
}
```

3. Configure Security
```java
@Configuration
@EnableWebSecurity
public class CustomSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private RequestParameterExtractorFilter requestParameterExtractorFilter;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/api/**").authenticated()
                .and()
            .addFilterBefore(requestParameterExtractorFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new CustomJwtFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
```

#### 2. Extending Security Features

When adding new security features:

1. Core Security Extensions
   - Create new components in `prs-middleware` if they are generic and reusable
   - Implement new validation logic that can be used across applications
   - Add new security filters that work with any endpoint configuration

2. Application-specific Security
   - Extend `CustomJwtAuthenticationConverter` for custom authentication logic
   - Create custom filters that use core security components
   - Implement application-specific validation rules

#### 3. Security Updates

When updating security features:

1. Core Security Updates
   - Update components in `prs-middleware` for security fixes
   - Enhance JWT validation logic
   - Improve certificate validation
   - All applications will automatically get these updates

2. Configuration Updates
   - Modify `EndpointConfig` for new security requirements
   - Update parameter validation rules
   - Add new security constraints

### Best Practices for Future Development

1. **Core Module Usage**
   - Always use core components from `prs-middleware` for security-critical operations
   - Extend rather than modify core components when custom behavior is needed
   - Follow the established security patterns and validation logic

2. **Endpoint Configuration**
   - Define custom endpoints in application-specific configuration
   - Use clear naming conventions for endpoints
   - Document security requirements for each endpoint

3. **Security Updates**
   - Test security updates thoroughly before deployment
   - Validate that core components work with all endpoint configurations
   - Document any breaking changes in configuration

This architecture provides a robust foundation for building secure applications while maintaining flexibility and reusability across different projects.

## Detailed Component Descriptions

### 1. `JwtValidator.java`

#### Responsibilities
- Parses and validates JWT tokens.
- Extracts and verifies the embedded JWK (JSON Web Key).
- Verifies JWT signature using ECDSA.
- Validates payload hash against expected concatenated parameters.

#### Key Methods
```java
public static JWK validateAndExtract(String jwtToken, String... params)
```

Validates the JWT and returns the embedded JWK. Parameters are used to compute the expected payload hash.

```java
public static String hashPayload(String input)
```

### 2. Payload Validation Solution

#### Problem Statement
The frontend generates a payload hash using specific parameters in a defined order, which must be validated against the JWT payload hash. For device validation endpoints, the parameters must be concatenated in the order: `initiationNonce`, `powHash`, `powNonce`.

#### Solution Components

1. **EndpointParameterMapper.java**
   - Maps API endpoints to their required parameters
   - Defines the exact order of parameters for each endpoint
   - Example configuration:
   ```java
   ENDPOINT_PARAMETERS.put("api/prs/dev/validate", Arrays.asList("initiationNonce", "powHash", "powNonce"));
   ENDPOINT_PARAMETERS.put("api/prs/otp/send", Arrays.asList("phoneNumber"));
   ENDPOINT_PARAMETERS.put("api/prs/email-otp/send", Arrays.asList("email", "accountId"));
   ENDPOINT_PARAMETERS.put("api/prs/kyc/info", Arrays.asList("idNumber", "expiryDate", "accountId"));
   ```

2. **RequestParameterExtractorFilter.java**
   - Extracts parameters from both POST and GET requests
   - Maintains parameter order using `LinkedHashMap`
   - Parameters are added in the exact order defined in EndpointParameterMapper
   - Works consistently for all endpoints
   - Key logic:
   ```java
   // Create ordered map to maintain parameter sequence
   Map<String, String> orderedParams = new LinkedHashMap<>();
   for (String paramName : requiredParams) {
       orderedParams.put(paramName, paramValue);
   }
   ```

3. **CachingRequestBodyWrapper.java**
   - Caches request body for POST requests
   - Allows multiple reads of the request body
   - Prevents `IllegalStateException` in Spring MVC
   - Ensures consistent parameter extraction

#### Implementation Steps

1. **Configure Endpoint Parameters**
   - Add endpoint mappings in `EndpointParameterMapper` with correct parameter order
   ```java
   ENDPOINT_PARAMETERS.put("api/prs/dev/validate", Arrays.asList("initiationNonce", "powHash", "powNonce"));
   ENDPOINT_PARAMETERS.put("api/prs/otp/send", Arrays.asList("phoneNumber"));
   ENDPOINT_PARAMETERS.put("api/prs/email-otp/send", Arrays.asList("email", "accountId"));
   ```

2. **Add Request Parameter Filter**
   - Register `RequestParameterExtractorFilter` in `SecurityConfig`
   - Ensure it runs before JWT validation
   ```java
   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
       http
               .addFilterBefore(requestParameterExtractorFilter, UsernamePasswordAuthenticationFilter.class)
               // ... other configurations
   }
   ```

3. **Verify Payload Hash**
   - Parameters are automatically passed to `JwtValidator.validateAndExtract()`
   - The validator concatenates parameters in the correct order
   - The hash is compared against the JWT payload hash

#### Best Practices
- Always define parameters in `EndpointParameterMapper` with the correct order
- Use consistent parameter names across frontend and backend
- Add logging to track parameter extraction and validation
- Test endpoints with different parameter combinations

#### Common Issues and Solutions

1. **Payload Hash Mismatch**
   - Verify parameter order in `EndpointParameterMapper`
   - Check frontend parameter concatenation
   - Add debug logging to track parameter values

2. **Request Body Reading Error**
   - Ensure `CachingRequestBodyWrapper` is used
   - Check filter chain order
   - Verify request body is not read before the filter

3. **Missing Parameters**
   - Add required parameters to `EndpointParameterMapper`
   - Check frontend parameter submission
   - Add validation in the filter

This solution ensures consistent payload validation across all endpoints while maintaining security and performance.

Computes SHA-256 hash of input data.

```java
public static String extractDeviceJwk(String jwtToken)
```

Extracts the device public key from the JWT header.

####  Payload Validation Implementation
The payload hash validation is now properly implemented through the integration of `RequestParameterExtractorFilter` with `EmbeddedJwkJwtDecoder`. The filter extracts request parameters and passes them to `JwtValidator.validateAndExtract()` via `RequestParameterExtractorFilter.getCurrentRequestParams()`, ensuring consistent payload validation across all endpoints.


### 2. `EmbeddedJwkJwtDecoder.java`

####  Responsibilities
- Wraps `JwtValidator.validateAndExtract()` to integrate with Spring Security's `JwtDecoder`.
- Returns a validated `Jwt` object usable for Spring Security authentication.

####  Key Method
```java
@Override
public Jwt decode(String token) throws JwtException
```

Calls `JwtValidator.validateAndExtract(token)` and constructs a Spring `Jwt` object from the parsed token.


### 3. `CertValidator.java`

#### Responsibilities
- Validates embedded certificates like `devJwt` or `accountJwt` found in JWT headers.
- Uses a public key loaded via `KeyLoader` to verify the certificate’s signature.

####  Key Method
```java
public boolean validateJWT(String jwtToken)
```

Parses the certificate from the JWT header and verifies its signature.


### 4. `CertValidationFilter.java`

#### Responsibilities
- Ensures that requests to endpoints requiring `ROLE_ACCOUNT_CERTIFIED` have a valid certificate.

####  Key Method
```java
@Override
protected void doFilterInternal(...)
```

Checks if user has `ROLE_ACCOUNT_CERTIFIED`, then validates the certificate using `CertValidator`.


### 5. `CustomJwtAuthenticationConverter.java`

#### Responsibilities
- Converts JWT into `JwtAuthenticationToken`.
- Adds custom authorities based on JWT headers and certificate validation:
  - Validates the `accountJwt` certificate using `CertValidator`
  - Grants `ROLE_ACCOUNT_CERTIFIED` only if the certificate is valid and present
  - Uses Spring's default authorities for other claims

#### Key Method
```java
private Collection<GrantedAuthority> extractAuthorities(Jwt jwt)
```

Validates the account certificate and adds appropriate authorities to the authentication token.

#### Key Method
```java
@Override
public JwtAuthenticationToken convert(Jwt jwt)
```

Adds `ROLE_ACCOUNT_CERTIFIED` if `accountJwt` header is present.

### 6. `SecurityConfig.java`

#### Responsibilities
- Enables Spring Security with OAuth2 Resource Server support.
- Configures URL-based access rules.
- Registers custom converters and filters.

####  Access Rules Summary

| Path | Required Authority |
|------|--------------------|
| `/api/prs/dev/**` | Authenticated only |
| `/api/prs/email-otp/**` | `ROLE_ACCOUNT_CERTIFIED` |
| `/api/prs/kyc/**` | `ROLE_ACCOUNT_CERTIFIED` |
| `/api/prs/otp/**` | `ROLE_ACCOUNT_CERTIFIED` |
| `/api/prs/recovery/**` | `ROLE_ACCOUNT_CERTIFIED` |
| All other paths | Authenticated only |


## Evolution: From Manual Validation to Spring Integration

### Before: Manual JWT Validation

Each endpoint manually validated JWT tokens and checked payloads:

```java
JwtValidator.validateAndExtract(jwtToken, email, accountId);
if (!certValidator.validateJWT(jwtToken)) {
    return "Invalid JWT";
}
```

This led to repetitive code and potential security inconsistencies.

### After: Spring Security Integration

Endpoints now leverage declarative security using annotations:

```java
@PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
```

Spring handles:
- Token decoding and validation.
- Role assignment based on JWT headers.
- Certificate validation via filter.


## Known Issue: Payload Hash Validation Fails

### Root Cause

The `EmbeddedJwkJwtDecoder` calls:

```java
JwtValidator.validateAndExtract(token); // No parameters!
```

But `validateAndExtract(...)` expects request-specific parameters (like `email`, `accountId`) to compute the expected payload hash.

### Impact

## Payload Validation Implementation

The payload validation system is implemented through a robust solution that combines several components:

1. **RequestParameterExtractorFilter**
   - Extracts parameters from HTTP requests (both GET and POST)
   - Maintains parameter order using `LinkedHashMap`
   - Stores parameters in ThreadLocal for access by `EmbeddedJwkJwtDecoder`

2. **EmbeddedJwkJwtDecoder Integration**
   - Retrieves parameters from `RequestParameterExtractorFilter.getCurrentRequestParams()`
   - Passes parameters to `JwtValidator.validateAndExtract()`
   - Ensures consistent validation across all endpoints

3. **EndpointParameterMapper**
   - Maps endpoints to their required parameters
   - Defines correct parameter order for each endpoint
   - Centralized configuration for maintainability

This implementation provides:
- **Security**: Consistent payload validation across all endpoints
- **Maintainability**: Centralized parameter configuration
- **Flexibility**: Supports both GET and POST requests
- **Performance**: Efficient parameter extraction and validation

The system is designed to provide robust payload validation for all endpoints while maintaining flexibility and performance.

## Developer Tips

- **Understand Spring Security**: Learn how Spring Security integrates with JWT via `JwtAuthenticationToken` and `JwtGrantedAuthoritiesConverter`.
- **Use Logging**: Extensive logging in `JwtValidator`, `CertValidator`, and `EmbeddedJwkJwtDecoder` helps debug validation issues.
- **Test JWTs**: Use tools like [jwt.io](https://jwt.io/) to generate test tokens and verify their structure.
- **Verify Roles**: Ensure `ROLE_ACCOUNT_CERTIFIED` is correctly assigned based on the presence of `accountJwt` in the header.



## Conclusion

The Webank security architecture provides a solid foundation for JWT and certificate validation integrated with Spring Security. By addressing the payload validation issue and leveraging Spring's declarative security model, the system remains both secure and scalable.

New developers should focus on understanding:
- How JWTs are validated and decoded.
- How roles are extracted and enforced.
- The current limitations around payload hash validation.
- How to extend the system with additional certificate types or validation logic.
