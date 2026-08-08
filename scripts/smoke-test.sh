#!/usr/bin/env bash
set -euo pipefail
BASE="${BASE_URL:-http://localhost:8080}"
CODE="smoke$RANDOM"
echo "Health"
curl -fsS "$BASE/actuator/health"; echo
echo "Create $CODE"
curl -fsS -X POST "$BASE/api/v1/links" -H 'Content-Type: application/json' -d "{\"url\":\"https://example.com/demo\",\"customAlias\":\"$CODE\"}"; echo
echo "Redirect"
curl -sS -o /dev/null -D - "$BASE/$CODE" | grep -E 'HTTP/|Location:'
echo "Analytics"
curl -fsS "$BASE/api/v1/links/$CODE/analytics"; echo
echo "Deactivate"
curl -fsS -o /dev/null -X DELETE "$BASE/api/v1/links/$CODE"
status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/$CODE")
[[ "$status" == "410" ]] || { echo "Expected 410, got $status"; exit 1; }
echo "Smoke test passed"
