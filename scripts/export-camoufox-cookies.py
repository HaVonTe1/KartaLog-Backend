#!/usr/bin/env python3
"""
Export cookies using Camoufox itself, so the cf_clearance matches Camoufox's
fingerprint. Runs non-headless once — you visit CardMarket, save cookies.

Usage:
    pip install camoufox playwright
    python scripts/export-camoufox-cookies.py
"""
import json
import os
from camoufox.sync_api import Camoufox

OUTPUT = os.getenv("OUTPUT", os.path.join(
    os.path.dirname(__file__) or ".", "..", "deployment", "cookies.json"
))

TARGET = "https://www.cardmarket.com"

print(f"Opening Camoufox browser to {TARGET} ...")
print("If Cloudflare shows a challenge, solve it manually.")
print("Otherwise, the page should load normally on your home IP.")
print("Close the browser window when done — cookies will be saved.\n")

with Camoufox(
    headless=False,
    geoip=True,
    humanize=True,
    i_know_what_im_doing=True,
    config={"forceScopeAccess": True},
    disable_coop=True,
    main_world_eval=True,
    locale="de-DE",
    os=["windows"],
    block_webrtc=True,
) as browser:
    ctx = browser.new_context(no_viewport=True)
    page = ctx.new_page()
    page.goto(TARGET, wait_until="networkidle")
    print(f"Page loaded: {page.title()}")
    print("Waiting for you to close the browser (Ctrl+C or close window)...")
    input("Press Enter when ready to save cookies and exit...")

    cookies = ctx.cookies()
    os.makedirs(os.path.dirname(OUTPUT) or ".", exist_ok=True)
    with open(OUTPUT, "w") as f:
        json.dump(cookies, f, indent=2)

    cf = [c for c in cookies if c.get("name") == "cf_clearance"]
    print(f"\nSaved {len(cookies)} cookies to {OUTPUT}")
    print(f"cf_clearance: {'✅ found' if cf else '❌ not found (not needed on your IP)'}")
