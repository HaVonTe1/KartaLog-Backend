# AGENTS.md – Deployment Module Guidelines

## OVERVIEW
- Purpose: package, deliver, and run TCGWatcher backend services.
- Artifacts: Docker images, DB init scripts, Helm/K8s manifests (optional).
- Audience: DevOps engineers, CI pipelines, on‑prem operators.

## DATABASE INIT SCRIPTS
- Location: `deployment/initdb/`.
- Scripts are plain SQL; idempotent where possible.
- Naming: `<ordinal>_<description>.sql` (e.g., `01_create_schema.sql`).
- Execution: `docker exec <db> psql -U $POSTGRES_USER -f /initdb/<script>.sql`.
- Versioning: keep scripts under Git, incremental only—no destructive drops.

## DOCKER / CONTAINER NOTES
- Base images: `openjdk:24-jdk-slim` for JVM runtime.
- Multi‑stage build: compile in `maven:3.9-jdk-24` stage, copy `target/*.jar` to runtime stage.
- Dockerfile placed in `deployment/docker/` per service (e.g., `app`, `worker`).
- Entrypoint: `java -jar /app.jar`.
- Healthcheck: `curl -f http://localhost:8080/actuator/health || exit 1`.
- Tagging scheme: `ghcr.io/<org>/tcgwatcher-<service>:<commit-sha>`.

## CI/CD HOOKS
- GitHub Actions workflow in `.github/workflows/deploy.yml`.
- Steps:
  1. `checkout` + cache Maven `~/.m2`.
  2. `mvn -q verify` (lint + tests).
  3. `docker build` for each service, push to GHCR.
  4. Deploy via Helm chart or `kubectl apply -f k8s/`.
- Secrets: `GHCR_TOKEN`, `DB_PASSWORD`, `KUBE_CONFIG`.
- Manual trigger allowed for staging/production pushes.

## LOGGING
- Container logs go to stdout/stderr; collected by Kubernetes/EFK stack.
- Use SLF4J with Logback; config at `src/main/resources/logback.xml`.
- Log level defaults to `INFO`; DEBUG enabled via env `LOG_LEVEL=DEBUG`.

## NOTES
- Keep Dockerfile and init scripts in sync; CI fails if build image cannot locate scripts.
- Do not commit generated `target/` or local Docker layers.
- For local dev, `docker compose up` reads `deployment/docker-compose.yml`.
- Security: never expose DB credentials; use `Docker secrets` or K8s `Secret` objects.
- Review image size (`docker image ls`) and trim unnecessary layers.
