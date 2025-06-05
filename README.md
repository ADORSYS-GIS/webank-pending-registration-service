# webank-pending-registration-service

## Getting Started

This project supports building native images using GraalVM Native Image. The native image provides faster startup times and lower memory footprint compared to running on the JVM.

## Prerequisites

- java 17
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

## Building and run Locally

## Environment Variables

The following environment variables are required, make to export them before running the application.

- `JWT_ISSUER`: JWT issuer URL (e.g., "<https:webank.com>")
- `JWT_EXPIRATION_TIME_MS`: JWT expiration time in milliseconds (e.g., "172800000" for 48 hours)
- `SERVER_PRIVATE_KEY_JSON`: Server private key in JWK format
- `SERVER_PUBLIC_KEY_JSON`: Server public key in JWK format
- `OTP_SALT`: A secure random string for OTP generation
- `EMAIL`: Email address for sending OTPs
- `PASSWORD`: Email password

### Start docker componse for external services

```bash
docker-compose up -d
```

### Using Maven

To build locally from the project root:

1. Build the native image

```bash
mvn clean package -Pnative -DskipTests
# Run the application
./prs/prs-rest-server/target/prs-rest-server
```

The native executable will be generated in `prs/prs-rest-server/target/prs-rest-server`.

2. Build the normal jar

```bash
mvn clean install
cd prs/prs-rest-server
mvn spring-boot:run
```


### Using Docker

To build using Docker:

```bash
#Make sure the environmental varaibles are exported
docker build -t webank-prs .
docker run -p 8080:8080 webank-prs

```

## Troubleshooting

If you encounter issueswith building or running webank-pending-registration-service:

1. Ensure environmental varaibles are correctly exported
2. Run docker compose is running for external services.
3. If running normal spring-boot ensure to run from `prs/prs-rest-server`

If you encounter issues with the native build:

1. Ensure SDKMAN is properly installed and configured
2. Verify GraalVM is installed and selected: `sdk current java`
3. Check the reflection configuration in `reflect-config.json`
4. Review the native-image.properties file for initialization settings
