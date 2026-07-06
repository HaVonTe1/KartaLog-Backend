#!/usr/bin/env python3
"""
Export cookies from a real browser session to use with Camoufox.

Runs a non-headless Chromium via Playwright, navigates to cardmarket.com,
waits for you to handle any Cloudflare challenge (there usually isn't one
on your home IP), then saves the cookies to cookies.json.

Usage:
    pip install playwright
    playwright install chromium
    python scripts/export-cookies.py

Then restart the app stack:
    docker compose -f deployment/compose.yml --profile deployment up -d
"""
import json
import os
import sys

from playwright.sync_api import sync_playwright

OUTPUT = os.getenv("OUTPUT", os.path.join(os.path.dirname(__file__) or ".", "..", "deployment", "cookies.json"))

TARGET = "https://www.cardmarket.com"

def main():
    print(f"Opening {TARGET} in a real browser window...")
    print("If Cloudflare shows a challenge, solve it manually.")
    print("The script will save cookies automatically once the page loads.")
    print()

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        context = browser.new_context()
        page = context.new_page()
        page.goto(TARGET, wait_until="networkidle")

        print(f"\nPage loaded: {page.title()}")
        has_challenge = "challenges.cloudflare.com" in page.content() or \
                        "Just a moment" in page.content() or \
                        "Nur einen Moment" in page.content()
        if has_challenge:
            print("⚠️  Cloudflare challenge detected! Solve it in the browser window,")
            print("   then press Enter here to continue...")
            input()
        else:
            print("✅ No Cloudflare challenge — page loaded directly.")
            print("   Press Enter to save cookies and exit...")
            input()

        cookies = context.cookies()
        os.makedirs(os.path.dirname(OUTPUT) or ".", exist_ok=True)
        with open(OUTPUT, "w") as f:
            json.dump(cookies, f, indent=2)

        cf = [c for c in cookies if c.get("name") == "cf_clearance"]
        print(f"\nSaved {len(cookies)} cookies to {OUTPUT}")
        if cf:
            print(f"✅ cf_clearance cookie found — Cloudflare will trust Camoufox!")
        else:
            print("ℹ️  No cf_clearance cookie (not needed if your IP isn't challenged)")

        browser.close()


if __name__ == "__main__":
    main()
