# webank-pending-registration-service

# Native Image Build

This project supports building native images using GraalVM Native Image. The native image provides faster startup times and lower memory footprint compared to running on the JVM.

## Prerequisites

- SDKMAN (for managing Java versions)
- Docker (for containerized builds)

## Setup

1. Install SDKMAN if not already installed:
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

2. Install and use GraalVM:
```bash
# List available GraalVM versions
sdk list java | grep graalvm

# Install GraalVM 22.3.3
sdk install java 22.3.3.r17-grl

# Use GraalVM for current session
sdk use java 22.3.3.r17-grl

# Install native-image component
gu install native-image

# Verify installation
java -version  # Should show GraalVM
native-image --version  # Should show native-image version
```

3. Enable GraalVM for the project:
```bash
# From project root
sdk env install
sdk use
```

## Building Locally

### Using Maven

To build the native image locally from the project root:

```bash
mvn -Pnative native:compile
```

The native executable will be generated in `prs/prs-rest-server/target/prs-rest-server`.

### Using Docker

To build using Docker:

```bash
docker build -t webank-prs-native .
```

## Running the Application

### Direct Execution

From the `prs/prs-rest-server` directory:

```bash
# Set required environment variables
export JWT_ISSUER="<your-jwt-issuer-url>"
export JWT_EXPIRATION_TIME_MS="<expiration-time-in-ms>"
export SERVER_PRIVATE_KEY_JSON='<your-private-key-jwk>'
export SERVER_PUBLIC_KEY_JSON='<your-public-key-jwk>'
export TWILIO_ACCOUNT_SID="<your-twilio-sid>"
export TWILIO_AUTH_TOKEN="<your-twilio-token>"
export TWILIO_PHONE_NUMBER="<your-twilio-phone>"
export OTP_SALT="<your-otp-salt>"
export EMAIL="<your-email>"
export PASSWORD="<your-email-password>"

# Run the application
./target/prs-rest-server
```

### Using Docker

```bash
docker run -p 8080:8080 \
  -e JWT_ISSUER="<your-jwt-issuer-url>" \
  -e JWT_EXPIRATION_TIME_MS="<expiration-time-in-ms>" \
  -e SERVER_PRIVATE_KEY_JSON='<your-private-key-jwk>' \
  -e SERVER_PUBLIC_KEY_JSON='<your-public-key-jwk>' \
  -e TWILIO_ACCOUNT_SID="<your-twilio-sid>" \
  -e TWILIO_AUTH_TOKEN="<your-twilio-token>" \
  -e TWILIO_PHONE_NUMBER="<your-twilio-phone>" \
  -e OTP_SALT="<your-otp-salt>" \
  -e EMAIL="<your-email>" \
  -e PASSWORD="<your-email-password>" \
  webank-prs-native
```

## Environment Variables

The following environment variables are required:

- `JWT_ISSUER`: JWT issuer URL (e.g., "https://your-domain.com")
- `JWT_EXPIRATION_TIME_MS`: JWT expiration time in milliseconds (e.g., "172800000" for 48 hours)
- `SERVER_PRIVATE_KEY_JSON`: Server private key in JWK format
- `SERVER_PUBLIC_KEY_JSON`: Server public key in JWK format
- `TWILIO_ACCOUNT_SID`: Twilio account SID from your Twilio dashboard
- `TWILIO_AUTH_TOKEN`: Twilio auth token from your Twilio dashboard
- `TWILIO_PHONE_NUMBER`: Your Twilio phone number in E.164 format (e.g., "+1234567890")
- `OTP_SALT`: A secure random string for OTP generation
- `EMAIL`: Email address for sending OTPs
- `PASSWORD`: Email password or app password (for Gmail, use an App Password)

## Performance Benefits

The native image provides several benefits:

- Faster startup time (typically 50-100x faster)
- Lower memory footprint
- Smaller container size
- Better resource utilization

## Troubleshooting

If you encounter issues with the native build:

1. Ensure SDKMAN is properly installed and configured
2. Verify GraalVM is installed and selected: `sdk current java`
3. Check the reflection configuration in `reflect-config.json`
4. Review the native-image.properties file for initialization settings

For more detailed error information, run the build with:

```bash
mvn -Pnative native:compile -Dnative.image.debug=true
```