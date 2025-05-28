
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
| `CertValidator` | Validates certificates (`devJwt`, `accountJwt`) embedded in JWT headers using public key verification. |
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

Computes SHA-256 hash of input data.

```java
public static String extractDeviceJwk(String jwtToken)
```

Extracts the device public key from the JWT header.

####  Known Issue
When used via `EmbeddedJwkJwtDecoder`, this method is called without parameters, causing payload hash validation to fail unless modified.


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
- Adds custom authorities based on JWT headers (e.g., `ROLE_ACCOUNT_CERTIFIED` if `accountJwt` exists).

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

Payload hash validation fails due to empty parameter list.

## Proposed Solutions

### Option 1: Optional Payload Validation

Modify `validateAndExtract(...)` to skip payload hash check when no parameters are provided:

```java
if (params.length > 0) {
    validatePayloadHash(payload, concatenatePayloads(params));
} else {
    logger.warn("No payload parameters provided; skipping payload hash validation");
}
```

**Pros:** Simple fix, maintains compatibility with Spring integration.  
**Cons:** Less secure for critical endpoints.


### Option 2: Request-Specific Parameter Extraction

Add logic in `EmbeddedJwkJwtDecoder` or a pre-filter to extract request body/path parameters dynamically and pass them to `validateAndExtract(...)`.

**Pros:** Maintains payload validation across all flows.  
**Cons:** Complex implementation, requires mapping between endpoints and required parameters.


### Option 3: Endpoint-Specific Validators

Keep manual validation in service layers where payload validation is crucial.

**Pros:** Flexible, granular control over validation per endpoint.  
**Cons:** Partial duplication of effort.


## Recommendation

Use **Option 1: Optional Payload Validation** for now. It allows smooth Spring integration while maintaining flexibility for endpoints that require strict payload checks.

Later, implement **Option 2** selectively for high-security endpoints like KYC or recovery.

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
