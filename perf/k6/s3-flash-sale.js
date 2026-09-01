// S3 — flash sale: up to ATTEMPTS offered attempts against a coupon limited to LIMIT,
// under full load. A performance-and-correctness test in one: exactly LIMIT requests
// must end with 201, and every other outcome must be an explainable rejection.
//
// Response taxonomy (client side):
//   201                              -> coupon_success        (must be exactly LIMIT)
//   409 COUPON_USAGE_LIMIT_REACHED   -> coupon_limit_reached  (pressure that reached the DB)
//   503 GEO_IP_LOOKUP_FAILED         -> coupon_geoip_rejected (fail-closed under overload:
//                                       the pool-less GeoIP client saturates at high rps;
//                                       nothing is persisted, invariants unaffected)
//   status 0 (transport failure)     -> coupon_client_error   (client-side view only)
//   anything else                    -> coupon_unexpected     (must be zero)
//
// ATTEMPTS is the OFFERED ceiling, not a guarantee — under saturation the generator
// drops iterations it cannot start. The `coupon_limit_reached >= LIMIT` threshold
// guards that real pressure reached the database (at least 2x LIMIT attempts made it
// to the transaction); without it a red `coupon_success` could mean "test underfed"
// rather than "invariant broken".
//
// k6 counters observe the CLIENT side. After the run, ./perf/verify-s3.sh <code>
// checks the invariants directly in the database and is the source of truth.
import http from 'k6/http';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:18080';
const RATE = Number(__ENV.RATE || 2000);
const LIMIT = Number(__ENV.LIMIT || 10000);
// 80k offered over 40 s: even at a few hundred tps of actual throughput the run
// still applies enough pressure to exhaust the limit with a wide margin on slower hosts.
const ATTEMPTS = Number(__ENV.ATTEMPTS || 80000);
const HEADERS = { 'Content-Type': 'application/json' };

const success = new Counter('coupon_success');
const limitReached = new Counter('coupon_limit_reached');
const geoIpRejected = new Counter('coupon_geoip_rejected');
const clientError = new Counter('coupon_client_error');
const unexpected = new Counter('coupon_unexpected');

export const options = {
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    flash_sale: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: `${Math.ceil(ATTEMPTS / RATE)}s`,
      preAllocatedVUs: 2000,
      maxVUs: 6000,
      // Longer than the request timeout, so in-flight requests finish and their
      // statuses are counted instead of being killed mid-flight.
      gracefulStop: '75s',
    },
  },
  thresholds: {
    coupon_success: [`count==${LIMIT}`],
    coupon_unexpected: ['count==0'],
    coupon_limit_reached: [`count>=${LIMIT}`],
  },
};

export function setup() {
  const code = `PERFS3F${Date.now()}`;
  const res = http.post(
    `${BASE}/api/v1/coupons`,
    JSON.stringify({ code, maxUsageCount: LIMIT, countryCode: 'PL' }),
    { headers: HEADERS },
  );
  if (res.status !== 201) {
    throw new Error(`setup: failed to create the coupon: ${res.status} ${res.body}`);
  }
  console.log(`S3 coupon code: ${code}  (after the run: ./perf/verify-s3.sh ${code})`);
  return { code };
}

export default function (data) {
  const res = http.post(
    `${BASE}/api/v1/coupons/redeem`,
    JSON.stringify({ code: data.code, userId: `u-${__VU}-${__ITER}` }),
    { headers: HEADERS, timeout: '60s' },
  );
  const body = String(res.body);
  if (res.status === 201) {
    success.add(1);
  } else if (res.status === 409 && body.includes('COUPON_USAGE_LIMIT_REACHED')) {
    limitReached.add(1);
  } else if (res.status === 503 && body.includes('GEO_IP_LOOKUP_FAILED')) {
    geoIpRejected.add(1);
  } else if (res.status === 0) {
    clientError.add(1);
  } else {
    unexpected.add(1);
  }
}
