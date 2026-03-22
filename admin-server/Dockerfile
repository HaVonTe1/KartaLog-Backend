# syntax=dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

COPY gradle gradle/
COPY gradlew .
COPY admin-server/build.gradle.kts  ./

RUN --mount=type=cache,target=/root/.gradle ./gradlew --version --no-daemon

COPY admin-server/src src/

RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
