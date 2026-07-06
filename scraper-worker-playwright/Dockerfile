FROM node:22-slim

RUN apt-get update && apt-get install -y \
    wget ca-certificates \
    --no-install-recommends && \
    npx playwright install chromium && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY package.json ./
RUN npm install
COPY index.js .

ENV PORT=3001
EXPOSE 3001

HEALTHCHECK --interval=30s --timeout=5s --retries=5 --start-period=15s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3001/health || exit 1

CMD ["node", "index.js"]
