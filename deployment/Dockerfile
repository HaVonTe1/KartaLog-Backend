FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

COPY gradle gradle/
COPY gradlew .
COPY build.gradle.kts settings.gradle.kts ./

RUN --mount=type=cache,target=/root/.gradle ./gradlew --version --no-daemon

COPY src src/
COPY contract contract/

RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y --no-install-recommends \
    nodejs \
    npm \
    chromium \
    chromium-driver \
    libgtk-3-0 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libxrender1 \
    libxcursor1 \
    libxi6 \
    libasound2t64 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libcairo2 \
    libcairo-gobject2 \
    libgdk-pixbuf-2.0-0 \
    libatk1.0-0t64 \
    libgbm1 \
    libnspr4 \
    libnss3 \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

ARG CAMOUFOX_VERSION=135.0.1-beta.24
RUN wget -q "https://github.com/daijro/camoufox/releases/download/v${CAMOUFOX_VERSION}/camoufox-${CAMOUFOX_VERSION}-lin.x86_64.zip" \
    && mkdir -p /opt/camoufox \
    && unzip -q "camoufox-${CAMOUFOX_VERSION}-lin.x86_64.zip" -d /opt/camoufox \
    && rm "camoufox-${CAMOUFOX_VERSION}-lin.x86_64.zip"

ENV LD_LIBRARY_PATH=/opt/camoufox:$LD_LIBRARY_PATH

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=0

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Dplaywright.nodejs.path=/usr/bin/node", "-Dplaywright.browser.validate.hostDependencies=false", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
