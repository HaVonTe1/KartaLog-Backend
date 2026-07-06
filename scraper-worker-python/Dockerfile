FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    curl \
    ca-certificates \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Install Camoufox
RUN wget -q "https://github.com/daijro/camoufox/releases/download/v135.0.1-beta.24/camoufox-135.0.1-beta.24-lin.x86_64.zip" && \
    mkdir -p /opt/camoufox && \
    unzip -q "camoufox-135.0.1-beta.24-lin.x86_64.zip" -d /opt/camoufox && \
    rm "camoufox-135.0.1-beta.24-lin.x86_64.zip"

# Install Playwright system deps
RUN pip install --no-cache-dir playwright && \
    playwright install-deps firefox chromium 2>/dev/null; \
    rm -rf /root/.cache/pip

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ app/

ENV PORT=3002
ENV HEADLESS=true

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -sf http://localhost:3002/health || exit 1

CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "3002"]
