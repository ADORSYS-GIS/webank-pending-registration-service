# WeBank Pending Registration Service API Documentation

## Overview

The WeBank Pending Registration Service provides a comprehensive REST API for managing user registration processes, including OTP verification, KYC (Know Your Customer) processes, and device registration.

## API Documentation Access

### Swagger UI
Once the application is running, you can access the interactive API documentation at:
- **Local**: http://localhost:8080/swagger-ui.html

### OpenAPI Specification
The raw OpenAPI 3.1 specification is available at:
- **Local**: http://localhost:8080/v3/api-docs

## Key Features

1. **Interactive Documentation**: Test API endpoints directly from the browser
2. **Authentication**: All endpoints require JWT Bearer token authentication
3. **Request/Response Examples**: Each endpoint includes example requests and responses
4. **Error Handling**: Standardized error responses with detailed error codes
5. **Non-authenticated Test Endpoints**: Special test endpoints for easier testing without authentication

## API Endpoints

### OTP Management (`/api/prs/otp`)
- `POST /send` - Generate and send OTP to phone number
- `POST /validate` - Validate OTP code

### KYC Processing (`/api/prs/kyc`)
- `POST /documents` - Submit KYC documents
- `POST /info` - Submit personal information
- `POST /location` - Submit location data
- `POST /email` - Submit and verify email
- `GET /pending` - Get pending KYC records
- `GET /findById/{id}` - Find KYC by document ID

### Email OTP (`/api/prs/email-otp`)
- `POST /send` - Send OTP to email address
- `POST /validate` - Validate email OTP

### Account Recovery (`/api/prs/recovery`)
- `POST /validate` - Validate recovery token and associate new account

### Device Registration (`/api/prs/device`)
- Various endpoints for device registration and management

### Test Endpoints (`/api/test`)
- `POST /otp/send` - Test OTP sending (no auth)
- `POST /otp/validate` - Test OTP validation (no auth)
- `POST /email-otp/send` - Test email OTP sending (no auth)
- `POST /kyc/documents` - Test KYC document submission (no auth)
- `POST /recovery/validate` - Test account recovery (no auth)
- `GET /generate-token` - Generate test JWT token

## Authentication

All API endpoints (except test endpoints) require JWT authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

For testing purposes, you can generate a test token using the `/api/test/generate-token` endpoint.

## Response Format

All responses follow a standardized format:

### Success Response
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    // Response data
  },
  "timestamp": "2025-01-20T15:30:00"
}
```

### Error Response
```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": "Additional error details",
  "timestamp": "2025-01-20T15:30:00",
  "path": "/api/prs/endpoint"
}
```

## Testing the API

### Using Test Endpoints
1. Start the application:
   ```bash
   mvn spring-boot:run
   ```

2. Navigate to http://localhost:8080/swagger-ui.html

3. Expand the "Test Endpoints" section

4. Select a test endpoint to try out

5. Click "Try it out" and modify the request as needed

6. Click "Execute" to send the request

### Using Authenticated Endpoints
1. Generate a test token using the `/api/test/generate-token` endpoint

2. Click on "Authorize" button in Swagger UI and enter your JWT token with the format: `Bearer eyJhbGciOiJIUzI1NiIs...`

3. Select an authenticated endpoint to test

4. Click "Try it out" and modify the request as needed

5. Click "Execute" to send the request

## Error Codes

| Code | Description |
|------|-------------|
| INVALID_OTP | The OTP is invalid or has expired |
| OTP_EXPIRED | OTP has expired |
| INVALID_PHONE | Invalid phone number format |
| INVALID_EMAIL | Invalid email format |
| VALIDATION_ERROR | Request validation failed |
| UNAUTHORIZED | Invalid or missing authentication token |
| INTERNAL_SERVER_ERROR | Unexpected server error |

## Development Notes

- All DTOs are documented with `@Schema` annotations
- Controllers use `@Operation` and `@ApiResponses` annotations
- Request validation is performed automatically
- Global exception handling ensures consistent error responses
- Test endpoints are available for trying out the API without authentication 