# scraper-worker-python — Camoufox Scraper Worker

**Stack:** Python 3.12 · FastAPI · Camoufox (0.4.10) · Playwright (1.49+)

## Overview
HTTP sidecar that uses Camoufox (stealth Firefox fork with anti-detection) to fetch CardMarket pages while bypassing Cloudflare Turnstile. Called by `CamoufoxPythonWorkerStrategy` in the Java backend at runtime.

## How It Works
1. **Fingerprint evasion:** Camoufox is configured with `locale="de-DE"`, `os=["windows"]`, `block_webrtc=True`, `geoip=True` — this mimics a German Windows user (CardMarket's core demographic). `humanize=True` adds realistic async delays to mouse/keyboard/interaction timing.
2. **Homepage warmup:** Before navigating to the search URL, the worker visits `cardmarket.com/de/Pokemon` first. This establishes session cookies and a referer chain, making the subsequent request look like a real browsing session. Without this warmup, Cloudflare often serves the hard Turnstile variant.
3. **Content detection:** Uses CardMarket-specific HTML markers (`ProductSearchInput`, `CardmarketNewsLink`) to confirm the page loaded correctly. These elements never appear on Cloudflare challenge pages.
4. **Recovery:** 12 attempts with `location.reload()` at attempt 4 and fresh navigation at attempt 8. Returns the challenge page HTML in a 503 response if never resolved.

## Key Files
| File | Purpose |
|------|---------|
| `app/main.py` | FastAPI app with `/health` and `/fetch` endpoints |
| `Dockerfile` | Multi-stage image, installs Camoufox binary + Playwright system deps |
| `requirements.txt` | Python dependencies |

## API
**`GET /health`** — `{"status": "ok", "browser": "connected"}`
**`POST /fetch`** — `{"url": "..."}` → `{"status": 200, "content": "...", "url": "..."}`

Error response: `{"status": 503, "error": "...", "content": "...", "url": "..."}`
Timeout: 120s (configurable via `NAV_TIMEOUT` env var)

## Critical Constraints
- `no_viewport=True` is required in `browser.new_context()` — Camoufox v0.4.x CDP does not support `isMobile`/`screenSize` fields that Playwright sends with a normal viewport, causing a Protocol error.
- `config={"forceScopeAccess": True}` is required for the `camoufox_add_init_script` addon to function. The addon forces `attachShadow` to open mode so `playwright-captcha`'s ClickSolver can access Turnstile's shadow DOM if needed.
- Keep `playwright-captcha` in requirements even if ClickSolver is not actively used — its `get_addon_path()` is the only way to get the Camoufox addon path.

## Known Issues
- Turnstile non-interactive variant (`/dark/fbE/new/normal`) cannot be solved via checkbox click. The `playwright-captcha` ClickSolver will click something in the iframe and cause navigation, but the challenge does not resolve. Current strategy avoids triggering this variant entirely via fingerprint tuning + warmup.
- **Session decay:** Camoufox success rate drops from ~92% (hour 1) to ~10% (hour 3) as the IP/fingerprint gets flagged by Cloudflare. The worker now restarts the entire browser instance on failure to get a fresh fingerprint. This is a stopgap — residential proxies are the permanent fix.

## Cookie Reuse (Bypass Cloudflare)
The worker can load cookies (especially `cf_clearance`) from your real Chrome browser
to skip Cloudflare challenges. Since your home IP is trusted by CardMarket, Chrome
gets no challenge. Exporting those cookies to Camoufox lets it piggyback on that trust.

### One-time setup
```bash
# Install Playwright locally (not in Docker)
pip install playwright
playwright install chromium

# Run the export script — it opens a browser, loads CardMarket, saves cookies
python scripts/export-cookies.py
```

This creates `deployment/cookies.json` which is auto-mounted into the container
via the compose file. The worker loads it on startup and applies the cookies
to every new browser context.

### Manual export (alternative)
1. Open Chrome, go to `cardmarket.com`
2. DevTools → Application → Cookies → `cardmarket.com`
3. Select all cookies, copy as JSON
4. Save as `deployment/cookies.json`

### Env Vars
| Var | Default | Description |
|-----|---------|-------------|
| `PORT` | 3002 | HTTP server port |
| `NAV_TIMEOUT` | 120000 | Page navigation timeout (ms) |
| `HEADLESS` | true | Browser headless mode |
| `COOKIES_FILE` | /app/cookies.json | Path to Playwright-format cookies JSON |
