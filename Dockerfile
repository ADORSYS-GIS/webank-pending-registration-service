FROM vegardit/graalvm-maven:latest-java17 AS builder

WORKDIR /build_dir

COPY pom.xml .
COPY prs/pom.xml prs/
COPY prs/prs-db-repository/pom.xml prs/prs-db-repository/
COPY prs/prs-middleware/pom.xml prs/prs-middleware/
COPY prs/prs-rest-api/pom.xml prs/prs-rest-api/
COPY prs/prs-rest-server/pom.xml prs/prs-rest-server/
COPY prs/prs-service-api/pom.xml prs/prs-service-api/
COPY prs/prs-service-impl/pom.xml prs/prs-service-impl/

# Download dependencies (this layer will be cached if pom.xml files don't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY prs/prs-db-repository/src prs/prs-db-repository/src
COPY prs/prs-middleware/src prs/prs-middleware/src
COPY prs/prs-rest-api/src prs/prs-rest-api/src
COPY prs/prs-rest-server/src prs/prs-rest-server/src
COPY prs/prs-service-api/src prs/prs-service-api/src
COPY prs/prs-service-impl/src prs/prs-service-impl/src

RUN \
    mvn package -Pnative -DskipTests \
    && cp prs/prs-rest-server/target/prs-rest-server ./server

FROM debian:12-slim AS deps

# Get build platform information
ARG TARGETPLATFORM
ARG BUILDPLATFORM

RUN apt-get update && apt-get install -y \
    zlib1g \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

# Determine architecture-specific paths
RUN case "${TARGETPLATFORM}" in \
        "linux/amd64") \
            ARCH_DIR="x86_64-linux-gnu" \
            LOADER="ld-linux-x86-64.so.2" \
            ;; \
        "linux/arm64") \
            ARCH_DIR="aarch64-linux-gnu" \
            LOADER="ld-linux-aarch64.so.1" \
            ;; \
        *) \
            echo "Unsupported platform: ${TARGETPLATFORM}" && exit 1 \
            ;; \
    esac && \
    echo "ARCH_DIR=${ARCH_DIR}" > /tmp/arch_info && \
    echo "LOADER=${LOADER}" >> /tmp/arch_info

# Create architecture-agnostic directory structure
RUN . /tmp/arch_info && \
    mkdir -p /deps/lib/${ARCH_DIR} /deps/lib64

# Copy architecture-specific libraries
RUN . /tmp/arch_info && \
    cp /lib/${ARCH_DIR}/libz.so.1 /deps/lib/${ARCH_DIR}/ && \
    cp /lib/${ARCH_DIR}/libc.so.6 /deps/lib/${ARCH_DIR}/ && \
    cp /lib64/${LOADER} /deps/lib64/

# Copy additional shared libraries (with error handling)
RUN . /tmp/arch_info && \
    find /lib/${ARCH_DIR} -name "*.so*" -exec cp {} /deps/lib/${ARCH_DIR}/ \; 2>/dev/null || true

FROM gcr.io/distroless/static-debian12:nonroot

WORKDIR /app

EXPOSE 8080

# Copy libraries (the target platform will determine the correct paths)
ARG TARGETPLATFORM
COPY --from=deps /deps/lib/ /lib/
COPY --from=deps /deps/lib64/ /lib64/

COPY --from=builder /build_dir/server .

ENTRYPOINT ["/app/server"]