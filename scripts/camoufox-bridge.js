const { firefox } = require('playwright');
const http = require('http');
const fs = require('fs');
const path = require('path');

const HEALTH_PORT = parseInt(process.env.CAMOUFOX_CDP_HEALTH_PORT || '9224', 10);
const WS_PORT = parseInt(process.env.CAMOUFOX_CDP_WS_PORT || '9226', 10);
const CAMOUFOX_BIN = process.env.CAMOUFOX_BIN || '';
const hasDisplay = !!(process.env.DISPLAY || process.env.WAYLAND_DISPLAY);
const HEADLESS = process.env.CAMOUFOX_HEADLESS === 'true' || (process.env.CAMOUFOX_HEADLESS !== 'false' && !hasDisplay);
const COOKIES_PATH = process.env.CAMOUFOX_COOKIES_PATH || path.join(__dirname, '..', 'deployment', 'cookies.json');

let browserServer;
let healthServer;

async function importCookies() {
  if (!fs.existsSync(COOKIES_PATH)) {
    console.log('No cookies.json found at ' + COOKIES_PATH + ' - skipping cookie import.');
    return;
  }

  let cookies;
  try {
    cookies = JSON.parse(fs.readFileSync(COOKIES_PATH, 'utf8'));
  } catch (e) {
    console.warn('Failed to parse cookies.json: ' + e.message);
    return;
  }
  if (!Array.isArray(cookies) || cookies.length === 0) {
    console.warn('cookies.json is empty - skipping.');
    return;
  }

  console.log('Importing ' + cookies.length + ' cookies from ' + COOKIES_PATH + '...');
  const wsEndpoint = browserServer.wsEndpoint();
  const browser = await firefox.connect(wsEndpoint);
  try {
    // viewport:null to skip setDefaultViewport (protocol mismatch with Camoufox Firefox)
    const context = await browser.newContext({ viewport: null });
    // Playwright's addCookies needs Playwright-compatible format
    const pwCookies = cookies.map(c => ({
      name: c.name,
      value: c.value,
      domain: c.domain,
      path: c.path || '/',
      httpOnly: c.httpOnly || false,
      secure: c.secure || false,
      sameSite: c.sameSite || 'Lax',
    }));
    await context.addCookies(pwCookies);
    console.log('Cookies imported. Warming up session...');
    // Visit cardmarket to warm up the session + cf_clearance
    const page = await context.newPage();
    await page.goto('https://www.cardmarket.com', { waitUntil: 'domcontentloaded', timeout: 30000 });
    const title = await page.title();
    console.log('Warm-up page title: ' + title);
    await page.close();
    await context.close();
  } finally {
    await browser.close();
  }
}

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
  browserServer = await firefox.launchServer({ ...launchOptions, port: WS_PORT });
  const wsEndpoint = browserServer.wsEndpoint();
  console.log('Camoufox Playwright server ready at: ' + wsEndpoint);

  // Import cookies into the persistent profile
  await importCookies();

  healthServer = http.createServer((req, res) => {
    if (req.url === '/') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        status: 'ok',
        wsEndpoint: wsEndpoint,
        browser: 'Camoufox (Firefox)',
        pid: browserServer.process().pid,
        hasCookies: fs.existsSync(COOKIES_PATH),
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
