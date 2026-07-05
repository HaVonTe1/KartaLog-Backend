# AGENTS.md – Deployment Module Guidelines

## OVERVIEW
- Purpose: package, deliver, and run KartaLog backend.
- Artifacts: Docker image, DB init scripts, compose profiles.
- Audience: DevOps engineers, CI pipelines, on‑prem operators.

## DATABASE INIT SCRIPTS
- Location: `deployment/postgres/initdb/`.
- Scripts are plain SQL; idempotent where possible.
- Naming: `<ordinal>_<description>.sql`.
- Versioning: keep scripts under Git, incremental only.

## DOCKER
- Single `deployment/Dockerfile` multi‑stage build.
- Builder: `eclipse-temurin:17-jdk-alpine` Gradle bootJar.
- Runtime: `eclipse-temurin:24-jre-alpine` + Node.js + Chromium (Playwright).
- Entrypoint: `java -Dplaywright.* -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/app.jar`.
- HEALTHCHECK: `wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health`

## COMPOSE
- `deployment/compose.yml` — profiles: `local` (postgres via `spring-boot-docker-compose`), `deployment` (full stack: app, nginx, admin-server, postgres, ofelia).
- Local dev: `SPRING_PROFILES_ACTIVE=compose ./gradlew bootRun` (port 8080).

## CI
- Self‑hosted runner on push/PR to `main`.
- Runs `./gradlew check` (unit + integration tests, ktlint on test sources).
- No deploy workflow exists yet.

## LOGGING
- Container logs to stdout/stderr.
- SLF4J + Logback; config at `src/main/resources/logback.xml`.
- Default `INFO`; debug via env `LOG_LEVEL=DEBUG`.

## BACKUP & RESTORE
- Scripts in `deployment/scripts/`: `backup.sh`, `restore.sh`.
- Automated via Ofelia cron in compose `deployment` profile (daily 2 AM).

## NOTES
- Database: PostgreSQL (runtime) + SQLite (quicksearch import).
- Separate DB users: `watcher_mig` (migrations), `watcher_app` (app), `watcher_readonly` (read-only).
- Do not commit `.env` or `auth.json`.
