FROM node:22-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
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
    libasound2 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libcairo2 \
    libcairo-gobject2 \
    libgdk-pixbuf-2.0-0 \
    libatk1.0-0 \
    libgbm1 \
    libnspr4 \
    libnss3 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package.json .
RUN npm install

COPY index.js .

ENV PORT=3000

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD node -e "require('http').get('http://localhost:3000/health',(r)=>{process.exit(r.statusCode!==200?1:0)}).on('error',(e)=>{process.exit(1)})"

CMD ["node", "index.js"]
