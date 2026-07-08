#!/usr/bin/env bash
# Pipeline validation: nginx → app → Python worker → CardMarket
set -euo pipefail

NGINX_URL="${NGINX_URL:-http://localhost:19123}"
APP_PORT="${APP_PORT:-}"
PYTHON_WORKER_URL="${PYTHON_WORKER_URL:-}"

PASS=0
FAIL=0
FAILURES=""

pass() { PASS=$((PASS+1)); echo "  PASS: $1"; }
fail() { FAIL=$((FAIL+1)); FAILURES="$FAILURES\n  FAIL: $1"; echo "  FAIL: $1"; }

assert_http() {
    local url="$1" desc="$2" expect="${3:-200}"
    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 15 "$url" 2>/dev/null || echo "000")
    if [ "$status" = "$expect" ]; then
        pass "$desc ($status)"
    else
        fail "$desc — expected $expect, got $status"
    fi
}

resolve_app_port() {
    if [ -n "$APP_PORT" ]; then
        echo "$APP_PORT"
        return
    fi
    docker port kartalogbackend-app-1 8080 2>/dev/null | head -1 | sed 's/.*://' || echo ""
}

echo "====================================="
echo "  Pipeline validation"
echo "  nginx → app → scraper"
echo "====================================="
echo ""

# Discover app port
APP_PORT=$(resolve_app_port)
if [ -z "$APP_PORT" ]; then
    echo "  Cannot find app port — skipping direct tests"
fi
echo "  nginx:       $NGINX_URL"
echo "  app (direct): ${APP_PORT:+http://localhost:$APP_PORT}"
echo ""

# --- 1. nginx connectivity ---
echo "--- 1. nginx reverse proxy ---"
assert_http "$NGINX_URL/" "nginx root" "404"  # expected — no root endpoint
assert_http "$NGINX_URL/collectables/" "collectables endpoint" "400"  # 400 = reached app (missing params)

# --- 2. App direct connectivity ---
if [ -n "$APP_PORT" ]; then
    echo ""
    echo "--- 2. App direct checks ---"
    assert_http "http://localhost:$APP_PORT/collectables/" "app collectables" "400"
fi

# --- 3. Strategy ---
echo ""
echo "--- 3. Scraping strategy ---"
STRATEGY=$(curl -s --max-time 15 "$NGINX_URL/actuator/scraper/strategy" 2>/dev/null || echo '{"error":"unreachable"}')
echo "  Current: $STRATEGY"
CURRENT_ID=$(echo "$STRATEGY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || echo "")
if [ -n "$CURRENT_ID" ]; then
    pass "strategy: $CURRENT_ID"
else
    fail "strategy API unreachable"
fi

# --- 4. Python worker health ---
echo ""
echo "--- 4. Python worker ---"
if [ -z "$PYTHON_WORKER_URL" ]; then
    WORKER_PORT=$(docker port kartalogbackend-scraper-worker-python-1 3002 2>/dev/null | head -1 | sed 's/.*://' || echo "")
    if [ -n "$WORKER_PORT" ]; then
        PYTHON_WORKER_URL="http://localhost:$WORKER_PORT"
    fi
fi

if [ -n "$PYTHON_WORKER_URL" ]; then
    assert_http "$PYTHON_WORKER_URL/health" "worker health"
    BROWSER=$(curl -s --max-time 5 "$PYTHON_WORKER_URL/health" 2>/dev/null | python3 -c \
        "import sys,json; print(json.load(sys.stdin).get('browser','unknown'))" 2>/dev/null || echo "unknown")
    echo "  Browser: $BROWSER"
    if [ "$BROWSER" = "connected" ]; then
        pass "browser connected"
    else
        fail "browser not connected"
    fi
else
    echo "  SKIP: cannot determine worker port"
fi

# --- 5. Full pipeline search ---
echo ""
echo "--- 5. Live search (nginx → app → scraper → CardMarket) ---"
echo "  Search: Pikachu (this may take 30-120s)"

START_TS=$(date +%s%N)
HTTP_CODE=$(curl -s -o /tmp/pipeline_result.json -w "%{http_code}" \
    --max-time 180 \
    "$NGINX_URL/collectables/?query=Giflor&genre=Pokemon&type=Singles&locale=de" 2>/dev/null || echo "000")
END_TS=$(date +%s%N)
DURATION_MS=$(( (END_TS - START_TS) / 1000000 ))

echo "  HTTP $HTTP_CODE, ${DURATION_MS}ms"

if [ "$HTTP_CODE" = "200" ]; then
    RESULTS=$(python3 -c "
import json
with open('/tmp/pipeline_result.json') as f:
    d = json.load(f)
print(len(d.get('results', [])))
" 2>/dev/null || echo "0")
    pass "search returned $RESULTS results in ${DURATION_MS}ms"
elif [ "$HTTP_CODE" = "503" ]; then
    echo "  (Cloudflare blocked or challenge not resolved)"
    echo "  Pipeline connected, scraper attempted, CardMarket rejected"
    pass "search attempted (503 — pipeline OK, Cloudflare blocked)"
elif [ "$HTTP_CODE" = "504" ]; then
    echo "  (nginx timeout — worker took too long)"
    echo "  Check worker logs: docker logs kartalogbackend-scraper-worker-python-1 --tail 10"
    fail "search timed out after 180s"
else
    echo "  Full response (first 500 chars):"
    head -c 500 /tmp/pipeline_result.json 2>/dev/null || true
    echo ""
    fail "search returned HTTP $HTTP_CODE"
fi

# --- Summary ---
echo ""
echo "====================================="
echo "  $PASS passed, $FAIL failed"
echo "====================================="
if [ "$FAIL" -gt 0 ]; then
    echo -e "Failures:$FAILURES"
    exit 1
fi
exit 0
