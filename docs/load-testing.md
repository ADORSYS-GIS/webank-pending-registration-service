# Load Testing Documentation

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Implementation Details](#implementation-details)
5. [Configuration](#configuration)
6. [Running Tests](#running-tests)
7. [Understanding Results](#understanding-results)
8. [CI/CD Integration](#cicd-integration)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

## Overview

This project implements load testing for the Webank Pending Registration Service using Artillery. The load testing suite is specifically designed to test the KYC (Know Your Customer) and device registration flows under various load conditions.

### Key Features
- Comprehensive KYC flow testing
- Device registration and validation
- OTP (One-Time Password) testing
- Email verification flows
- Account recovery scenarios
- Realistic data generation
- Multiple environment support
- CI/CD integration with GitHub Actions

## Prerequisites

Before running the load tests, ensure you have:

1. **Node.js** (v18 or higher)
2. **npm** (v8 or higher)
3. **Artillery** installed globally:
   ```bash
   npm install -g artillery
   ```

## Project Structure

```
qa/load-tests/
├── load-test.yml           # Main load test configuration
├── functions.js           # Helper functions for test data generation
├── run-tests.sh          # Shell script for running tests
└── package.json          # Dependencies and scripts
```

## Implementation Details

### Test Scenarios

The load test suite implements several key scenarios:

1. **KYC Document Flow**
   - Personal information submission
   - Document upload (front ID, back ID, selfie, tax ID)
   - KYC certificate retrieval
   - Document status checking

2. **KYC Location Flow**
   - Location data submission
   - Geographic coordinates validation

3. **KYC Email Flow**
   - Email verification
   - Email status updates

4. **OTP Flow**
   - OTP generation and sending
   - OTP validation
   - Pending OTP list retrieval

5. **Email OTP Flow**
   - Email OTP generation
   - Email OTP validation

6. **Account Recovery Flow**
   - Recovery token generation
   - Account recovery validation

### Data Generation

The test suite includes sophisticated data generation through `functions.js`:

1. **Key Pair Generation**
   ```javascript
   function generateTestKeyPair(context, events, done) {
       // Generates EC key pairs for JWT signing
   }
   ```

2. **Test Data Generation**
   ```javascript
   function generateTestData(context, events, done) {
       // Generates realistic test data including:
       // - Customer IDs
       // - Phone numbers
       // - Email addresses
       // - Account IDs
   }
   ```

3. **JWT Token Generation**
   ```javascript
   function generateJWT(context, events, done) {
       // Generates signed JWT tokens for authentication
   }
   ```

## Configuration

### Environment Configuration

The load tests support multiple environments:

```yaml
environments:
  dev:
    target: "https://dev.webank.gis.ssegning.com"
  local:
    target: "http://localhost:8080"
```

### Load Phases

```yaml
phases:
  - duration: 15
    arrivalRate: 2
    rampTo: 5
    name: "Ramp up load"
  - duration: 15
    arrivalRate: 3
    name: "Sustained load"
```

### Performance Thresholds

```yaml
plugins:
  expect:
    - maxErrorRate: 5
    - maxResponseTime: 2000
```

## Running Tests

### Local Execution

1. Using the shell script:
   ```bash
   ./run-tests.sh
   ```

2. Direct npm commands:
   ```bash
   npm run test:dev    # For dev environment
   npm run test        # For local environment
   ```

### Environment Variables

- `TARGET_URL`: API endpoint URL
- `ENV`: Environment selection (local/dev)

## Understanding Results

### Test Metrics

The load test generates comprehensive metrics:

1. **Response Times**
   - Median response time
   - P95 response time
   - P99 response time

2. **Request Rates**
   - Total requests per scenario
   - Requests per second
   - Failed requests count

3. **Error Rates**
   - HTTP status code distribution
   - Error percentages per endpoint

### Reports

1. **JSON Report**
   - Detailed metrics per endpoint
   - Scenario-specific statistics
   - Error details

2. **HTML Report**
   - Visual graphs of response times
   - Request rate charts
   - Error distribution visualization

## CI/CD Integration

### GitHub Actions Workflow

The load testing is integrated into the CI/CD pipeline through `.github/workflows/load-test.yml`:

1. **Triggers**
   - Push to main branch
   - Pull requests to main
   - Manual trigger through workflow_dispatch

2. **Environment Selection**
   - Configurable through workflow inputs
   - Defaults to dev environment

3. **Artifact Generation**
   - JSON results
   - HTML reports

## Best Practices

1. **Test Data Management**
   - Use realistic data patterns
   - Implement proper data cleanup
   - Avoid hardcoded test data

2. **Performance Thresholds**
   - Set acceptable response times (2000ms)
   - Define error rate limits (5%)
   - Monitor resource usage

3. **Test Scenarios**
   - Start with smoke tests
   - Gradually increase load
   - Include error scenarios

## Troubleshooting

### Common Issues

1. **Test Failures**
   - Check target URL configuration
   - Verify environment variables
   - Review network connectivity

2. **Report Generation**
   - Ensure proper file permissions
   - Check disk space
   - Verify JSON report generation

3. **CI/CD Issues**
   - Check GitHub Actions logs
   - Verify secrets configuration
   - Review workflow permissions

### Debugging Steps

1. Enable verbose logging:
   ```bash
   artillery run -e dev --debug load-test.yml
   ```

2. Check report generation:
   ```bash
   artillery report load-test-report.json
   ```

3. Verify environment variables:
   ```bash
   echo $TARGET_URL
   ```

## Contributing

When adding new test scenarios or modifying existing ones:

1. Follow the established patterns in `functions.js`
2. Update documentation for new features
3. Test locally before committing
4. Ensure CI/CD pipeline compatibility
