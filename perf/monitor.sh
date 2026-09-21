#!/usr/bin/env bash
# Samples the inside of the SUT once per second during a test: busy Tomcat worker
# threads, Hikari pool state (pending > 0 = threads waiting for a connection) and
# Postgres sessions waiting on a lock (coupon-row contention made visible).
# Output: CSV on stdout — redirect to a file and line it up with the k6 timeline.
# When the actuator is unresponsive a sample takes up to ~7 s (3 x curl --max-time 2
# + the psql probe); the widening timestamp gaps are themselves a saturation signal.
# Stop with Ctrl+C.
set -euo pipefail

for tool in curl jq docker; do
  command -v "$tool" >/dev/null 2>&1 || { echo "monitor.sh: missing required tool: $tool" >&2; exit 1; }
done

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
COMPOSE=(docker compose -f "$SCRIPT_DIR/compose.perf.yml")

# Host port 19090 -> container 9090 = the separate management port (perf profile):
# the actuator keeps answering there even when application threads are saturated.
APP=${APP_URL:-http://localhost:19090}

metric() {
  curl -sf --max-time 2 "$APP/actuator/metrics/$1" | jq -r '.measurements[0].value' 2>/dev/null || echo NaN
}

echo "ts,tomcat_busy,hikari_active,hikari_pending,pg_lock_waits"
while true; do
  busy=$(metric tomcat.threads.busy)
  active=$(metric hikaricp.connections.active)
  pending=$(metric hikaricp.connections.pending)
  locks=$("${COMPOSE[@]}" exec -T postgres psql -U postgres -d coupon_redemption_service -tAc \
    "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock'" 2>/dev/null || echo NaN)
  echo "$(date +%H:%M:%S),$busy,$active,$pending,$locks"
  sleep 1
done
