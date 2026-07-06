#!/usr/bin/env bash
# Start Chrome with remote debugging for the Chrome CDP strategy.
# Uses socat to expose port 0.0.0.0 because Chrome 127+ ignores
# --remote-debugging-address=0.0.0.0 on Linux.
#
# Run this on your host (not in Docker).
# Keep this terminal open while scraping.
#
# Usage: bash scripts/start-chrome-cdp.sh

set -euo pipefail

PORT="${CHROME_CDP_PORT:-9222}"
SOCAT_PORT="${CHROME_CDP_SOCAT_PORT:-9223}"

if ! command -v socat &>/dev/null; then
    echo "socat is required. Install it:"
    echo "  sudo apt install socat"
    exit 1
fi

# Detect Chrome binary
CHROME=""
for bin in google-chrome google-chrome-stable chromium chromium-browser; do
    if command -v "$bin" &>/dev/null; then
        CHROME="$bin"
        break
    fi
done

if [ -z "$CHROME" ]; then
    echo "Chrome not found. Install Google Chrome or Chromium."
    exit 1
fi

echo "Starting $CHROME with remote debugging on port $PORT..."
echo ""

"$CHROME" \
    --remote-debugging-port="$PORT" \
    --remote-allow-origins=* \
    --no-first-run \
    --no-default-browser-check \
    --user-data-dir="$HOME/.config/chrome-cdp-scraper" \
    --disable-sync \
    --disable-translate \
    >/dev/null 2>&1 &

PID=$!
echo "Chrome started (PID: $PID)"

# Give Chrome a moment to bind
sleep 1

echo "Starting socat forward: 0.0.0.0:$SOCAT_PORT -> 127.0.0.1:$PORT"
echo ""
socat TCP-LISTEN:"$SOCAT_PORT",fork,reuseaddr TCP:127.0.0.1:"$PORT" &
SOCAT_PID=$!

echo "Verify (host):         curl http://localhost:$PORT/json/version"
echo "Verify (from Docker):  curl http://host.docker.internal:$SOCAT_PORT/json/version"
echo ""
echo "To stop later: kill $PID $SOCAT_PID"
