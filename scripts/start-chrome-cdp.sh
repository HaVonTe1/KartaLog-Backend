#!/usr/bin/env bash
# Start Chrome with remote debugging for the Chrome CDP strategy.
# Uses socat to expose port 0.0.0.0 because Chrome 127+ ignores
# --remote-debugging-address=0.0.0.0 on Linux.
#
# Run this on your host (not in Docker).
#
# Usage: bash scripts/start-chrome-cdp.sh

set -euo pipefail

PORT="${CHROME_CDP_PORT:-9222}"
SOCAT_PORT="${CHROME_CDP_SOCAT_PORT:-9223}"
DATA_DIR="${CHROME_CDP_DATA_DIR:-$HOME/.config/chrome-cdp-scraper}"
VERIFY_TIMEOUT="${CHROME_CDP_VERIFY_TIMEOUT:-15}"

if ! command -v socat &>/dev/null; then
    echo "ERROR: socat is required. Install it:"
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
    echo "ERROR: Chrome not found. Install Google Chrome or Chromium."
    exit 1
fi

if lsof -i ":$PORT" -sTCP:LISTEN 2>/dev/null; then
    echo "WARN: Port $PORT is already in use. Checking if it's a Chrome DevTools endpoint..."
    if curl -sf "http://localhost:$PORT/json/version" >/dev/null 2>&1; then
        echo "OK: Chrome is already running with remote debugging on port $PORT."
        echo "     Skipping Chrome launch."
        CHROME_ALREADY_RUNNING=true
    else
        echo "ERROR: Port $PORT is in use by another process."
        echo "       Free it or set CHROME_CDP_PORT to a different port."
        exit 1
    fi
else
    CHROME_ALREADY_RUNNING=false
fi

mkdir -p "$DATA_DIR"
# Clean stale lock files from unclean shutdown (Chrome refuses to start if these exist)
rm -f "$DATA_DIR/SingletonLock" "$DATA_DIR/SingletonSocket" "$DATA_DIR/SingletonCookie"

if [ "$CHROME_ALREADY_RUNNING" = false ]; then
    echo "Starting $CHROME with remote debugging on port $PORT..."
    echo "  User data dir: $DATA_DIR"

    "$CHROME" \
        --remote-debugging-port="$PORT" \
        --remote-allow-origins=* \
        --no-first-run \
        --no-default-browser-check \
        --user-data-dir="$DATA_DIR" \
        --disable-sync \
        --disable-translate \
        >/dev/null 2>&1 &

    CHROME_PID=$!

    # Disown so Chrome survives terminal close
    disown "$CHROME_PID" 2>/dev/null || true

    echo "Waiting for Chrome to bind port $PORT (timeout: ${VERIFY_TIMEOUT}s)..."
    VERIFY_START=$SECONDS
    while true; do
        if curl -sf "http://localhost:$PORT/json/version" >/dev/null 2>&1; then
            echo "OK: Chrome (PID $CHROME_PID) is listening on port $PORT."
            break
        fi
        if ! kill -0 "$CHROME_PID" 2>/dev/null; then
            echo "ERROR: Chrome process (PID $CHROME_PID) died during startup."
            echo "       Check Chrome's stderr below (last 20 lines):"
            # Chrome may have written errors to its stderr before we redirected it
            echo "       (output was redirected to /dev/null — re-run without redirects to see errors)"
            echo ""
            echo "  Possible causes:"
            echo "    - Stale profile lock (already cleaned above)"
            echo "    - Snap confinement restricting access to $DATA_DIR"
            echo "    - Another Chrome instance with the same user data dir still running"
            echo "  Try: rm -rf \"$DATA_DIR\" && bash \"$0\""
            exit 1
        fi
        elapsed=$(( SECONDS - VERIFY_START ))
        if [ "$elapsed" -ge "$VERIFY_TIMEOUT" ]; then
            echo "ERROR: Chrome (PID $CHROME_PID) did not bind port $PORT within ${VERIFY_TIMEOUT}s."
            kill "$CHROME_PID" 2>/dev/null || true
            exit 1
        fi
        sleep 1
    done
else
    CHROME_PID=$(lsof -ti ":$PORT" -sTCP:LISTEN 2>/dev/null | head -1)
    echo "Using existing Chrome (PID $CHROME_PID)."
fi

SOCAT_PID=$(lsof -ti ":$SOCAT_PORT" -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$SOCAT_PID" ]; then
    echo "Stopping existing socat on port $SOCAT_PORT (PID $SOCAT_PID)..."
    kill "$SOCAT_PID" 2>/dev/null || true
    sleep 1
fi

echo "Starting socat forward: 0.0.0.0:$SOCAT_PORT -> 127.0.0.1:$PORT"
socat TCP-LISTEN:"$SOCAT_PORT",fork,reuseaddr TCP:127.0.0.1:"$PORT" &
SOCAT_PID=$!
disown "$SOCAT_PID" 2>/dev/null || true

echo ""
echo "All up. Quick checks:"
echo "  Host:          curl http://localhost:$PORT/json/version"
echo "  From Docker:   curl http://172.17.0.1:$SOCAT_PORT/json/version"
echo ""
echo "To stop later: kill $CHROME_PID $SOCAT_PID"
