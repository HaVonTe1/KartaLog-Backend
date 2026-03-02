FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

COPY gradle gradle/
COPY gradlew .
COPY build.gradle.kts settings.gradle.kts ./
COPY src src/
COPY contract contract/

RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:24-jre-alpine

RUN apk add --no-cache nodejs npm chromium

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Dplaywright.nodejs.path=/usr/bin/node", "-Dplaywright.browser.validate.hostDependencies=false", "-Dplaywright.chromium.executablePath=/usr/bin/chromium", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
