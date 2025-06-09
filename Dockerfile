FROM vegardit/graalvm-maven:latest-java17 AS builder

WORKDIR /build_dir

COPY . .

RUN \
    mvn -Pnative install \
    && cp prs/prs-rest-server/target/prs-rest-server ./server

FROM debian:12-slim AS deps

RUN apt-get update && apt-get install -y \
    zlib1g \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

# Create dependency directories and copy all required libraries in one layer
RUN mkdir -p /deps/lib /deps/lib64 /deps/usr-lib && \
    find /lib/*-linux-gnu -name "*.so*" -exec cp {} /deps/lib/ \; 2>/dev/null || true && \
    find /usr/lib/*-linux-gnu -name "*.so*" -exec cp {} /deps/usr-lib/ \; 2>/dev/null || true && \
    cp /lib64/ld-linux-*.so.* /deps/lib64/ 2>/dev/null || true && \
    find /lib -maxdepth 1 -name "*.so*" -exec cp {} /deps/lib/ \; 2>/dev/null || true

FROM gcr.io/distroless/static-debian12:nonroot

WORKDIR /app

COPY --from=deps /deps/lib/ /lib/
COPY --from=deps /deps/usr-lib/ /usr/lib/
COPY --from=deps /deps/lib64/ /lib64/

COPY --from=builder /build_dir/server .

ENTRYPOINT ["/app/server"]