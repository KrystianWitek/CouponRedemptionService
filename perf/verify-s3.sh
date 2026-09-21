#!/usr/bin/env bash
# Post-S3 (flash sale) invariant verification, straight from the database — the
# source of truth for correctness (k6 counters only observe the client side).
#
# Checks two separate statements:
#   1. INVARIANT: coupon.current_usage_count == count(coupon_redemption rows).
#      A mismatch means a real correctness bug -> exit 1.
#   2. EXHAUSTION: both equal max_usage_count. If the invariant holds but the limit
#      was not reached, the run most likely under-applied pressure (see the
#      `http_reqs` guard threshold and dropped_iterations in k6) -> exit 2.
#
# Usage: ./verify-s3.sh <COUPON_CODE>   (code is printed by the S3 setup)
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
COMPOSE=(docker compose -f "$SCRIPT_DIR/compose.perf.yml")
CODE=${1:?usage: ./verify-s3.sh <COUPON_CODE>}

sql() {
  # The query is fed via stdin: psql does not interpolate :'var' variables in -c mode,
  # but does for scripts read from standard input.
  "${COMPOSE[@]}" exec -T postgres psql -U postgres -d coupon_redemption_service \
    -v code="$CODE" -tA <<< "$1"
}

coupon=$(sql "SELECT max_usage_count || '|' || current_usage_count FROM coupon WHERE code = upper(:'code')")
if [[ -z "$coupon" ]]; then
  echo "ERROR: coupon not found: $CODE" >&2
  exit 1
fi
max=${coupon%%|*}
counter=${coupon##*|}
rows=$(sql "SELECT count(*) FROM coupon_redemption r JOIN coupon c ON c.id = r.coupon_id WHERE c.code = upper(:'code')")

echo "coupon:            $CODE"
echo "limit (max_usage): $max"
echo "counter (current): $counter"
echo "redemption rows:   $rows"

if [[ "$counter" != "$rows" ]]; then
  echo "INVARIANT VIOLATION: usage counter and redemption rows disagree!" >&2
  exit 1
fi
if [[ "$counter" != "$max" ]]; then
  echo "Invariant holds (counter == rows), but the limit was NOT exhausted ($counter/$max):"
  echo "the run likely under-applied pressure — check http_reqs and dropped_iterations in k6."
  exit 2
fi
echo "OK: counter == redemption rows == limit. Invariants held under load."
