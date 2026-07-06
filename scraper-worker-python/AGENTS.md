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

## Env Vars
| Var | Default | Description |
|-----|---------|-------------|
| `PORT` | 3002 | HTTP server port |
| `NAV_TIMEOUT` | 120000 | Page navigation timeout (ms) |
| `HEADLESS` | true | Browser headless mode |
