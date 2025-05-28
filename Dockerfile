# Simplified Multi-stage Dockerfile
# This version expects artifacts to be built externally (in CI or locally)
# Build argument to control runtime type (jvm or native)
ARG BUILD_TYPE=jvm

# Runtime stage for JVM
FROM eclipse-temurin:17-jre-alpine AS jvm-runtime

WORKDIR /webank-prs

# Copy JAR file (should be built externally)
COPY ./prs/prs-rest-server/target/prs-rest-server-0.0.1-SNAPSHOT.jar /webank-prs/prs-rest-server-0.0.1-SNAPSHOT.jar

# Set environment variables
ENV OTP_SALT=${OTP_SALT}
ENV SERVER_PRIVATE_KEY_JSON=${SERVER_PRIVATE_KEY_JSON}
ENV SERVER_PUBLIC_KEY_JSON=${SERVER_PUBLIC_KEY_JSON}
ENV JWT_ISSUER=${JWT_ISSUER}
ENV JWT_EXPIRATION_TIME_MS=${JWT_EXPIRATION_TIME_MS}
ENV SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
ENV SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
ENV SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}

EXPOSE 8080

# Create non-root user for security
RUN addgroup -g 1001 -S prsgroup && \
    adduser -u 1001 -S prsuser -G prsgroup

USER prsuser

# Run the JVM application
CMD ["java", "-jar", "/webank-prs/prs-rest-server-0.0.1-SNAPSHOT.jar"]

# Runtime stage for Native
FROM alpine:3.18 AS native-runtime

# Install required runtime libraries for native image
RUN apk add --no-cache \
    libc6-compat \
    libstdc++ \
    && rm -rf /var/cache/apk/*

WORKDIR /webank-prs

# Copy native executable (should be built externally)
COPY ./prs/prs-rest-server/target/prs-rest-server /webank-prs/prs-rest-server

# Set environment variables
ENV OTP_SALT=${OTP_SALT}
ENV SERVER_PRIVATE_KEY_JSON=${SERVER_PRIVATE_KEY_JSON}
ENV SERVER_PUBLIC_KEY_JSON=${SERVER_PUBLIC_KEY_JSON}
ENV JWT_ISSUER=${JWT_ISSUER}
ENV JWT_EXPIRATION_TIME_MS=${JWT_EXPIRATION_TIME_MS}
ENV SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
ENV SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
ENV SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}

EXPOSE 8080

# Create non-root user for security
RUN addgroup -g 1001 prsgroup && \
    adduser -u 1001 -D prsuser -G prsgroup

# Make executable and set ownership
RUN chmod +x /webank-prs/prs-rest-server
RUN chown -R prsuser:prsgroup /webank-prs

USER prsuser

# Run the native application
CMD ["/webank-prs/prs-rest-server"]

# Final stage - select runtime based on build argument
FROM ${BUILD_TYPE}-runtime AS final