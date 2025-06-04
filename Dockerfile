FROM vegardit/graalvm-maven:latest-java17 AS builder

WORKDIR /build_dir

COPY . .

RUN \
  --mount=type=bind,source=./pom.xml,target=./pom.xml \
  --mount=type=bind,source=./prs/pom.xml,target=./prs/pom.xml \
  --mount=type=bind,source=./prs/prs-db-repository/pom.xml,target=./prs/prs-db-repository/pom.xml \
  --mount=type=bind,source=./prs/prs-db-repository/src,target=./prs/prs-db-repository/src \
  --mount=type=bind,source=./prs/prs-rest-api/pom.xml,target=./prs/prs-rest-api/pom.xml \
  --mount=type=bind,source=./prs/prs-rest-api/src,target=./prs/prs-rest-api/src \
  --mount=type=bind,source=./prs/prs-rest-server/pom.xml,target=./prs/prs-rest-server/pom.xml \
  --mount=type=bind,source=./prs/prs-rest-server/src,target=./prs/prs-rest-server/src \
  --mount=type=bind,source=./prs/prs-service-api/pom.xml,target=./prs/prs-service-api/pom.xml \
  --mount=type=bind,source=./prs/prs-service-api/src,target=./prs/prs-service-api/src \
  --mount=type=bind,source=./prs/prs-service-impl/pom.xml,target=./prs/prs-service-impl/pom.xml \
  --mount=type=bind,source=./prs/prs-service-impl/src,target=./prs/prs-service-impl/src \
  --mount=type=cache,target=./prs/prs-db-repository/target \
  --mount=type=cache,target=./prs/prs-rest-api/target \
  --mount=type=cache,target=./prs/prs-rest-server/target \
  --mount=type=cache,target=./prs/prs-service-api/target \
  --mount=type=cache,target=./prs/prs-service-impl/target \
  --mount=type=cache,target=/root/.m2/repository \
    mvn -Pnative install \
    && cp prs/prs-rest-server/target/prs-rest-server ./server 

FROM gcr.io/distroless/static-debian12:nonroot

WORKDIR /app

COPY --from=builder /build_dir/server .

ENTRYPOINT ["/app/server"]