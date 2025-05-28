# Exception Handling System Documentation

## Overview
The Webank Pending Registration Service implements a comprehensive exception handling system to ensure consistent error responses across all endpoints. This document outlines the exception handling architecture, custom exceptions, and their usage.

## Global Exception Handler

### `GlobalExceptionHandler`
Located at `com.webank.prs.rest.server.error.GlobalExceptionHandler`, this class provides centralized exception handling for the entire application.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Exception handling methods
}
```

### Key Features
- Centralized exception handling using Spring's `@RestControllerAdvice`
- Consistent error response format across all endpoints
- Proper HTTP status code mapping
- Detailed error messages for debugging
- Logging of all exceptions

## Custom Exceptions

### 1. Validation Exceptions

#### `ValidationException`
- **Purpose**: Handles input validation errors
- **HTTP Status**: 400 Bad Request
- **Usage**: When request parameters fail validation
- **Example**: Invalid phone number format, missing required fields

#### `DeviceValidationException`
- **Purpose**: Handles device-specific validation errors
- **HTTP Status**: 400 Bad Request
- **Usage**: When device registration or validation fails
- **Example**: Invalid device public key, PoW verification failure

#### `NonceValidationException`
- **Purpose**: Handles nonce-related validation errors
- **HTTP Status**: 400 Bad Request
- **Usage**: When nonce validation fails
- **Example**: Expired nonce, invalid nonce format

### 2. Resource Exceptions

#### `ResourceNotFoundException`
- **Purpose**: Handles missing resource errors
- **HTTP Status**: 404 Not Found
- **Usage**: When requested resource doesn't exist
- **Example**: Account not found, KYC record not found

### 3. Security Exceptions

#### `HashComputationException`
- **Purpose**: Handles hash computation errors
- **HTTP Status**: 500 Internal Server Error
- **Usage**: When hash generation fails
- **Example**: Invalid salt, algorithm not found

#### `FailedToSendOTPException`
- **Purpose**: Handles OTP sending failures
- **HTTP Status**: 500 Internal Server Error
- **Usage**: When OTP delivery fails
- **Example**: Email sending failure, SMS sending failure

## Error Response Format

All exceptions are converted to a standardized `ErrorResponse` format:

```json
{
    "timestamp": "2024-03-20T10:00:00.000+00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Detailed error message",
    "path": "/api/v1/endpoint"
}
```

## Exception Handling Flow

1. **Request Processing**
   - Request received by controller
   - Input validation performed
   - Business logic executed

2. **Exception Occurrence**
   - Exception thrown during processing
   - Caught by `GlobalExceptionHandler`

3. **Error Response Generation**
   - Exception mapped to appropriate HTTP status
   - Error details formatted into `ErrorResponse`
   - Response returned to client

## Best Practices

1. **Exception Selection**
   - Use specific exceptions for different error types
   - Avoid generic exceptions when possible
   - Include meaningful error messages

2. **Error Logging**
   - Log all exceptions with appropriate level
   - Include stack trace for debugging
   - Add context information when available

3. **Security Considerations**
   - Don't expose sensitive information in error messages
   - Sanitize error details for production
   - Use appropriate HTTP status codes

## Usage Examples

### 1. Input Validation
```java
if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
    throw new ValidationException("Invalid phone number format");
}
```

### 2. Resource Not Found
```java
Optional<PersonalInfoEntity> personalInfo = repository.findByAccountId(accountId);
if (personalInfo.isEmpty()) {
    throw new ResourceNotFoundException("No record found for accountId " + accountId);
}
```

### 3. Device Validation
```java
if (!powHash.equals(newPowHash)) {
    throw new DeviceValidationException("Verification of PoW failed");
}
```

## Testing

The exception handling system includes comprehensive test coverage:

1. **Unit Tests**
   - Test each custom exception
   - Verify error response format
   - Validate HTTP status codes

2. **Integration Tests**
   - Test exception handling in real scenarios
   - Verify logging behavior
   - Check error response consistency

## Maintenance

1. **Adding New Exceptions**
   - Create new exception class
   - Add handler method in `GlobalExceptionHandler`
   - Update documentation
   - Add test cases

2. **Modifying Existing Exceptions**
   - Update handler method
   - Maintain backward compatibility
   - Update documentation
   - Update test cases

## Troubleshooting

Common issues and solutions:

1. **Incorrect HTTP Status**
   - Check exception handler mapping
   - Verify exception inheritance
   - Review status code constants

2. **Missing Error Details**
   - Check exception constructor
   - Verify message formatting
   - Review logging configuration

3. **Inconsistent Error Format**
   - Check `ErrorResponse` structure
   - Verify handler method implementation
   - Review response serialization 