import asyncio
import logging
import os

from camoufox import AsyncCamoufox
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from playwright_captcha.utils.camoufox_add_init_script.add_init_script import get_addon_path

PORT = int(os.getenv("PORT", "3002"))
NAV_TIMEOUT = int(os.getenv("NAV_TIMEOUT", "120000"))
HEADLESS = os.getenv("HEADLESS", "true").lower() == "true"
# Camoufox addon that forces shadowRoot to open mode for Turnstile iframe access.
# Required by playwright-captcha ClickSolver if used; kept for compatibility.
ADDON_PATH = os.path.abspath(get_addon_path())

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("camoufox-worker")

camoufox: AsyncCamoufox | None = None
browser = None
_cookies: list | None = None


_restart_count = 0
_restart_lock = None

COOKIES_FILE = os.getenv("COOKIES_FILE", "")


def load_cookies_from_file():
    global _cookies
    if not COOKIES_FILE or not os.path.exists(COOKIES_FILE):
        logger.info("No cookies file found, continuing without pre-loaded cookies")
        return
    try:
        import json
        with open(COOKIES_FILE) as f:
            _cookies = json.load(f)
        cf = [c for c in _cookies if c.get("name") == "cf_clearance"]
        logger.info(f"Loaded {len(_cookies)} cookies ({len(cf)} cf_clearance) from {COOKIES_FILE}")
    except Exception as e:
        logger.warning(f"Failed to load cookies from {COOKIES_FILE}: {e}")


async def get_restart_lock():
    global _restart_lock
    if _restart_lock is None:
        _restart_lock = asyncio.Lock()
    return _restart_lock

async def start_browser():
    global camoufox, browser, _restart_count
    _restart_count += 1
    logger.info(f"Starting Camoufox (instance #{_restart_count})...")
    # Fingerprint tuned for cardmarket.de: German locale, Windows OS, WebRTC
    # blocked. These settings make the browser appear as a typical German
    # CardMarket user, reducing the chance of triggering a non-interactive
    # Turnstile challenge (the /fbE variant that ClickSolver cannot solve).
    # geoip=True + locale="de-DE" give a German geolocation.
    # humanize=True adds realistic async delays between actions.
    camoufox = AsyncCamoufox(
        headless=HEADLESS,
        geoip=True,
        humanize=True,
        i_know_what_im_doing=True,
        config={"forceScopeAccess": True},
        disable_coop=True,
        main_world_eval=True,
        locale="de-DE",
        os=["windows"],
        block_webrtc=True,
    )
    browser = await camoufox.start()
    logger.info("Camoufox ready")


async def stop_browser():
    global camoufox, browser
    if camoufox:
        await camoufox.__aexit__(None, None, None)
        camoufox = browser = None
    logger.info("Camoufox stopped")


async def restart_browser():
    async with await get_restart_lock():
        if browser is None:
            logger.info("Browser already stopped, skipping restart")
            return
        logger.info("Restarting Camoufox for fresh fingerprint...")
        await stop_browser()
        await start_browser()


app = FastAPI()


@app.on_event("startup")
async def startup():
    load_cookies_from_file()
    await start_browser()


@app.on_event("shutdown")
async def shutdown():
    await stop_browser()


# Accept both GET and HEAD (Docker healthcheck uses HEAD with curl -sf).
@app.api_route("/health", methods=["GET", "HEAD"])
async def health():
    return {"status": "ok", "browser": "connected" if browser else "disconnected"}


# Detects Cloudflare challenge pages by looking for markers in the HTML.
# Covers multiple languages (DE "Nur einen Moment", FR "Un instant", RU
# "Один момент") and Turnstile/challenge platform identifiers.
def is_challenge_page(content: str) -> bool:
    markers = [
        "challenges.cloudflare.com",
        "Just a moment",
        "Один момент",
        "Un instant",
        "cdn-cgi/challenge-platform",
        "Turnstile",
        "turnstile",
        "__cf_chl_tk",
        "cf_chl_opt",
    ]
    for m in markers:
        if m in content:
            return True
    return False


# Identifies actual CardMarket content by checking for HTML elements unique
# to CardMarket's UI (not present in Cloudflare challenge pages).
# ProductSearchInput = search bar element ID; CardmarketNewsLink = nav link.
def is_cardmarket_content(content: str) -> bool:
    return "ProductSearchInput" in content or "CardmarketNewsLink" in content


# Polls page content until real CardMarket HTML is detected or the challenge
# page clears. Blocks on the challenge page for up to `timeout` seconds.
async def wait_for_content(page, url: str, timeout: int = 60) -> tuple[str, str]:
    for i in range(timeout):
        await asyncio.sleep(1)
        content = await page.content()
        if is_cardmarket_content(content):
            logger.info(f"CardMarket content detected after {i+1}s")
            return content, page.url
        if not is_challenge_page(content):
            logger.info(f"Challenge page gone (not challenge content) after {i+1}s")
            return content, page.url
        if i % 10 == 0:
            logger.info(f"Still challenge page at {i+1}s, url={page.url}")
    logger.warning(f"Timeout waiting for content after {timeout}s")
    return await page.content(), page.url


@app.post("/fetch")
async def fetch(request: Request):
    body = await request.json()
    url = body.get("url")
    if not url:
        return JSONResponse(status_code=400, content={"error": "url required"})
    if not browser:
        return JSONResponse(status_code=503, content={"error": "browser not ready"})

    logger.info(f"Fetch: {url}")
    # no_viewport=True avoids a Camoufox CDP protocol error where
    # Browser.setDefaultViewport rejects the "isMobile" and "screenSize"
    # fields that Playwright sends with a normal viewport.
    ctx = await browser.new_context(no_viewport=True)
    if _cookies:
        try:
            await ctx.add_cookies(_cookies)
            logger.info(f"Applied {len(_cookies)} pre-loaded cookies to context")
        except Exception as e:
            logger.warning(f"Failed to apply cookies: {e}")
    page = await ctx.new_page()

    try:
        await page.goto(url, timeout=NAV_TIMEOUT, wait_until="load")
        await asyncio.sleep(3)

        for attempt in range(16):
            content = await page.content()
            cur_url = page.url

            if is_cardmarket_content(content):
                logger.info(f"Got content on attempt {attempt+1}")
                return {"status": 200, "content": content, "url": cur_url}

            if not is_challenge_page(content):
                logger.info(f"Challenge cleared attempt {attempt+1}, url={cur_url}")
                c2, u2 = await wait_for_content(page, url, timeout=15)
                return {"status": 200, "content": c2, "url": u2}

            logger.info(f"Challenge persists attempt {attempt+1}/16, url={cur_url}")

            if attempt == 5:
                logger.info("Doing location.reload()")
                try:
                    await page.evaluate("() => location.reload()")
                    await asyncio.sleep(2)
                except Exception:
                    pass
            elif attempt == 10:
                logger.info("Fresh navigation")
                try:
                    await page.goto(url, timeout=NAV_TIMEOUT, wait_until="load")
                except Exception:
                    pass

            await asyncio.sleep(3)

        logger.error("Challenge not resolved after 16 attempts — restarting browser")
        final_content = await page.content()
        final_url = page.url
        await page.close()
        await ctx.close()
        # Restart the entire browser to get a fresh fingerprint. Research shows
        # Camoufox session success drops from ~92% (hour 1) to ~10% (hour 3) as
        # the IP/fingerprint gets flagged. A full browser restart resets both.
        asyncio.create_task(restart_browser())
        return JSONResponse(
            status_code=200,
            content={
                "status": 503,
                "error": "Cloudflare challenge not resolved",
                "content": final_content,
                "url": final_url,
            },
        )
    except Exception as e:
        logger.error(f"Failed: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})
    finally:
        try:
            await page.close()
        except Exception:
            pass
        try:
            await ctx.close()
        except Exception:
            pass


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PORT)
