# Stage 1: Dependencies only
FROM debian:bullseye-slim AS dep
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libstdc++6 \
        zlib1g \
        libgcc1 && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir /deps && \
    cp /usr/lib/x86_64-linux-gnu/libstdc++.so.6 /deps/ && \
    cp /lib/x86_64-linux-gnu/libz.so.1 /deps/ && \
    cp /lib/x86_64-linux-gnu/libgcc_s.so.1 /deps/

# Stage 2: Final image
FROM gcr.io/distroless/base-debian12:nonroot
WORKDIR /webank-prs

# Copy the pre-built native executable from host
COPY prs/prs-rest-server/target/prs-rest-server /webank-prs/prs-rest-server
COPY prs/prs-rest-server/target/lib*.so* /usr/lib/

# Copy dependencies
COPY --from=dep /deps/libstdc++.so.6 /usr/lib/
COPY --from=dep /deps/libz.so.1 /usr/lib/
COPY --from=dep /deps/libgcc_s.so.1 /usr/lib/

USER nonroot
ENTRYPOINT ["/webank-prs/prs-rest-server"]