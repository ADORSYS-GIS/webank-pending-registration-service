
#  Webank Security Architecture Documentation

## Introduction

This document outlines the security components implemented in the `webank` project under the packages:

- `com.adorsys.webank.security`
- `com.adorsys.webank.config`

These components handle JWT validation, certificate validation, and Spring Security integration to secure REST endpoints such as email OTP, KYC, and device registration. The goal is to ensure JWT authenticity, integrity, and payload consistency while leveraging Spring Security for robust authentication and authorization.

This guide is designed for new developers joining the team, offering a clear understanding of each component's purpose, functionality, and interactions.


## Overview of Security Components

| Component | Purpose |
|----------|---------|
| `JwtValidator` | Validates JWTs by checking signature, embedded JWK, and payload hash. |
| `EmbeddedJwkJwtDecoder` | Integrates with Spring Security OAuth2 resource server to decode and validate JWT tokens. |
| `CertValidator` | Validates certificates ( `accountJwt`) embedded in JWT headers using public key verification. |
| `CertValidationFilter` | A Spring filter that enforces certificate validation for specific secured endpoints. |
| `CustomJwtAuthenticationConverter` | Customizes authority extraction from JWT headers (e.g., grants `ROLE_ACCOUNT_CERTIFIED`). |
| `SecurityConfig` | Main Spring Security configuration defining access rules, CORS, and JWT integration. |

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
