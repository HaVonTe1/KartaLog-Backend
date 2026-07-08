const { firefox } = require('playwright');
const http = require('http');

const HEALTH_PORT = parseInt(process.env.CAMOUFOX_CDP_HEALTH_PORT || '9224', 10);
const WS_PORT = parseInt(process.env.CAMOUFOX_CDP_WS_PORT || '9226', 10);
const CAMOUFOX_BIN = process.env.CAMOUFOX_BIN || '';
const hasDisplay = !!(process.env.DISPLAY || process.env.WAYLAND_DISPLAY);
const HEADLESS = process.env.CAMOUFOX_HEADLESS === 'true' || (process.env.CAMOUFOX_HEADLESS !== 'false' && !hasDisplay);

let browserServer;
let healthServer;

async function main() {
  const launchOptions = {
    executablePath: CAMOUFOX_BIN || undefined,
    headless: HEADLESS,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
    ],
  };

  if (!HEADLESS && !hasDisplay) {
    console.warn('WARN: No DISPLAY set. Camoufox will not have a visible window.');
    console.warn('  Set CAMOUFOX_HEADLESS=true for headless mode,');
    console.warn('  or install Xvfb and run: xvfb-run bash scripts/start-camoufox-cdp.sh');
  }

  console.log('Launching Camoufox (' + (CAMOUFOX_BIN || 'default firefox') + ')...');
  browserServer = await firefox.launchServer({...launchOptions, port: WS_PORT });
  const wsEndpoint = browserServer.wsEndpoint();
  console.log('Camoufox Playwright server ready at: ' + wsEndpoint);

  healthServer = http.createServer((req, res) => {
    if (req.url === '/') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        status: 'ok',
        wsEndpoint: wsEndpoint,
        browser: 'Camoufox (Firefox)',
        pid: browserServer.process().pid,
      }));
    } else {
      res.writeHead(404);
      res.end();
    }
  });
  healthServer.listen(HEALTH_PORT, '0.0.0.0', function() {
    console.log('Health endpoint: http://0.0.0.0:' + HEALTH_PORT + '/');
  });
}

const shutdown = async () => {
  console.log('Shutting down Camoufox bridge...');
  if (healthServer) healthServer.close();
  if (browserServer) await browserServer.close();
  process.exit(0);
};
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

main().catch((err) => {
  console.error('Failed to start Camoufox bridge:', err);
  process.exit(1);
});
