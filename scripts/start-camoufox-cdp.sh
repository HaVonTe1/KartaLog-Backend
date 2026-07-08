#!/usr/bin/env bash
# Start Camoufox (Firefox fork) as a remote Playwright server for the
# CamoufoxCdpStrategy.
#
# Uses Node.js Playwright's firefox.launchServer() to expose a Playwright
# protocol WebSocket that the Java app connects to via
# playwright.firefox().connect(wsUrl).
#
# Run this on your host (not in Docker).
#
# Usage: bash scripts/start-camoufox-cdp.sh

set -euo pipefail

HEALTH_PORT="${CAMOUFOX_CDP_HEALTH_PORT:-9224}"
WS_PORT="${CAMOUFOX_CDP_WS_PORT:-9226}"
SOCAT_PORT="${CAMOUFOX_CDP_SOCAT_PORT:-9225}"
SOCAT_HEALTH_PORT="${CAMOUFOX_CDP_SOCAT_HEALTH_PORT:-9227}"
BRIDGE_DIR="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS_DIR="$BRIDGE_DIR"

# Locate Camoufox binary
CAMOUFOX_BIN=""
for path in \
    "$HOME/.cache/camoufox/camoufox-bin" \
    /opt/camoufox/camoufox-bin \
    /tmp/camoufox-venv/bin/camoufox-bin \
    "$HOME/.local/share/camoufox/camoufox-bin"; do
    if [ -x "$path" ]; then
        CAMOUFOX_BIN="$path"
        break
    fi
done

if [ -z "$CAMOUFOX_BIN" ]; then
    echo "ERROR: Camoufox binary not found."
    echo "  Download from: https://github.com/daijro/camoufox/releases"
    echo "  Or set CAMOUFOX_CDP_BIN to the path."
    exit 1
fi
echo "Camoufox binary: $CAMOUFOX_BIN"

CAMOUFOX_BIN="${CAMOUFOX_CDP_BIN:-$CAMOUFOX_BIN}"

if ! command -v node &>/dev/null; then
    echo "ERROR: Node.js is required. Install it first."
    exit 1
fi

if ! command -v socat &>/dev/null; then
    echo "ERROR: socat is required. Install it:"
    echo "  sudo apt install socat"
    exit 1
fi

# Install Playwright in a local temp location if not available
BRIDGE_DEPS="$SCRIPTS_DIR/.camoufox-bridge-deps"
if [ ! -d "$BRIDGE_DEPS/node_modules/playwright" ]; then
    echo "Installing Playwright (Node.js) for the bridge..."
    mkdir -p "$BRIDGE_DEPS"
    cat > "$BRIDGE_DEPS/package.json" <<'PKGJSON'
{
  "name": "camoufox-cdp-bridge",
  "private": true,
  "type": "module",
  "dependencies": {
    "playwright": "^1.61.0"
  }
}
PKGJSON
    (cd "$BRIDGE_DEPS" && npm install --no-audit --no-fund 2>&1 | tail -3)
fi

# Check port availability
if lsof -i ":$HEALTH_PORT" -sTCP:LISTEN 2>/dev/null; then
    if curl -sf "http://localhost:$HEALTH_PORT/" >/dev/null 2>&1; then
        echo "OK: Camoufox bridge is already running (health on port $HEALTH_PORT)."
        echo "     Skipping bridge launch."
        BRIDGE_ALREADY_RUNNING=true
    else
        echo "ERROR: Port $HEALTH_PORT is in use by another process."
        echo "       Free it or set CAMOUFOX_CDP_HEALTH_PORT to a different port."
        exit 1
    fi
else
    BRIDGE_ALREADY_RUNNING=false
fi

if [ "$BRIDGE_ALREADY_RUNNING" = false ]; then
    echo "Starting Camoufox bridge..."
    echo "  Binary: $CAMOUFOX_BIN"
    echo "  Health HTTP: port $HEALTH_PORT"
    echo "  Playwright WS: port $WS_PORT"
    echo "  Bridge deps: $BRIDGE_DEPS"

    nohup env \
    NODE_PATH="$BRIDGE_DEPS/node_modules" \
    CAMOUFOX_BIN="$CAMOUFOX_BIN" \
    CAMOUFOX_CDP_HEALTH_PORT="$HEALTH_PORT" \
    CAMOUFOX_CDP_WS_PORT="$WS_PORT" \
    node "$SCRIPTS_DIR/camoufox-bridge.js" > "$BRIDGE_DIR/camoufox-bridge.log" 2>&1 &
    BRIDGE_PID=$!
    disown "$BRIDGE_PID" 2>/dev/null || true

    echo "Waiting for bridge health endpoint on port $HEALTH_PORT (timeout: 15s)..."
    for i in $(seq 1 15); do
        if curl -sf "http://localhost:$HEALTH_PORT/" >/dev/null 2>&1; then
            echo "OK: Camoufox bridge (PID $BRIDGE_PID) healthy on port $HEALTH_PORT."
            break
        fi
        if ! kill -0 "$BRIDGE_PID" 2>/dev/null; then
            echo "ERROR: Bridge process (PID $BRIDGE_PID) died."
            exit 1
        fi
        sleep 1
    done
else
    BRIDGE_PID=$(lsof -ti ":$HEALTH_PORT" -sTCP:LISTEN 2>/dev/null | head -1)
    echo "Using existing bridge (PID $BRIDGE_PID)."
fi

SOCAT_PID=$(lsof -ti ":$SOCAT_PORT" -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$SOCAT_PID" ]; then
    echo "Stopping existing socat on port $SOCAT_PORT (PID $SOCAT_PID)..."
    kill "$SOCAT_PID" 2>/dev/null || true
    sleep 1
fi

echo "Starting socat forward: 0.0.0.0:$SOCAT_PORT -> [::1]:$WS_PORT"
nohup socat TCP-LISTEN:"$SOCAT_PORT",fork,reuseaddr TCP6:[::1]:"$WS_PORT" > /dev/null 2>&1 &
SOCAT_PID=$!
disown "$SOCAT_PID" 2>/dev/null || true

echo "Starting socat forward for health: 0.0.0.0:$SOCAT_HEALTH_PORT -> 127.0.0.1:$HEALTH_PORT"
nohup socat TCP4-LISTEN:"$SOCAT_HEALTH_PORT",fork,reuseaddr TCP4:127.0.0.1:"$HEALTH_PORT" > /dev/null 2>&1 &
SOCAT_HEALTH_PID=$!
disown "$SOCAT_HEALTH_PID" 2>/dev/null || true

echo ""
echo "All up. Quick checks:"
echo "  Health:        curl http://localhost:$HEALTH_PORT/"
echo "  From Docker:   curl http://172.17.0.1:$SOCAT_PORT/  (WS -> $WS_PORT)"
echo "  Health Docker: curl http://172.17.0.1:$SOCAT_HEALTH_PORT/"
echo ""
echo "To stop later: kill $BRIDGE_PID $SOCAT_PID $SOCAT_HEALTH_PID"
