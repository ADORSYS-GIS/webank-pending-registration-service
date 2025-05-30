# Native Build Stage (GraalVM)

FROM ghcr.io/graalvm/native-image-community:21.3 AS native-builder

WORKDIR /app

# Copy full source (or adjust if using pre-built artifacts)
COPY . .

# OPTIONAL: Ensure dependencies are available
# RUN mvn -pl prs/prs-rest-server -am clean package -Pnative -DskipTests

# Build native executable
RUN native-image \
    --no-fallback \
    --enable-url-protocols=http,https \
    -H:Name=prs-rest-server \
    -cp prs/prs-rest-server/target/classes:$(echo prs/prs-rest-server/target/dependency/*.jar | tr ' ' ':')

# Native Runtime Stage (Distroless)

FROM gcr.io/distroless/base-debian12 AS final

WORKDIR /webank-prs

# Copy native binary
COPY --from=native-builder /app/prs-rest-server /webank-prs/prs-rest-server

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

# Run the native binary
CMD ["/webank-prs/prs-rest-server"]
