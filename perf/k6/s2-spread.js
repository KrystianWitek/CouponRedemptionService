// S2 — Traffic spread across many coupons: a realistic shop profile.
// Single-row contention disappears, which moves the ceiling elsewhere: measured runs
// put it at application CPU and at the pool-less GeoIP HTTP client (it opens a fresh
// connection per lookup and exhausts the container's ephemeral ports), not at the
// Hikari pool — a high hikari_pending here counts waiting THREADS, not a full pool.
// Same tagged-scenario structure as S1: `low` must stay green, `high` is expected
// to saturate and its failures are the measurement.
import http from 'k6/http';
import { check } from 'k6';
import { logScenario } from './scenario-banner.js';

const BASE = __ENV.BASE_URL || 'http://localhost:18080';
const RATE_LOW = Number(__ENV.RATE_LOW || 300);
const RATE_HIGH = Number(__ENV.RATE_HIGH || 3000);
const HOLD_S = toSeconds(__ENV.HOLD || '2m');
const WARMUP_S = 30;
const RAMP_S = 30;
const COUPONS = Number(__ENV.COUPONS || 1000);
const HEADERS = { 'Content-Type': 'application/json' };

function toSeconds(value) {
  const match = /^(\d+)(s|m)$/.exec(value);
  if (!match) throw new Error(`HOLD must look like '20s' or '2m', got: ${value}`);
  return Number(match[1]) * (match[2] === 'm' ? 60 : 1);
}

export const options = {
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    warmup: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 500,
      stages: [{ target: RATE_LOW, duration: `${WARMUP_S}s` }],
    },
    low: {
      executor: 'constant-arrival-rate',
      rate: RATE_LOW,
      timeUnit: '1s',
      duration: `${HOLD_S}s`,
      startTime: `${WARMUP_S}s`,
      preAllocatedVUs: 300,
      maxVUs: 1000,
    },
    high: {
      executor: 'ramping-arrival-rate',
      startRate: RATE_LOW,
      timeUnit: '1s',
      startTime: `${WARMUP_S + HOLD_S}s`,
      preAllocatedVUs: 1000,
      maxVUs: 4000,
      stages: [
        { target: RATE_HIGH, duration: `${RAMP_S}s` },
        { target: RATE_HIGH, duration: `${HOLD_S}s` },
      ],
    },
  },
  thresholds: {
    'http_req_duration{scenario:low,expected_response:true}': ['p(95)<200', 'p(99)<500'],
    'checks{scenario:low}': ['rate>0.999'],
    'dropped_iterations{scenario:low}': ['count<100'],
  },
};

export function setup() {
  logScenario({
    id: 'S2',
    name: 'Traffic spread across many coupons',
    checks: 'what limits throughput once single-row contention is gone',
    expects: `'low' meets the SLO; 'high' saturates on application CPU and the GeoIP client`,
    load: `${RATE_LOW} then ${RATE_HIGH} rps, ${COUPONS} coupons at random, ${HOLD_S}s plateaus`,
    watch: 'hikari_active (not pending), process CPU, 503 GEO_IP_LOOKUP_FAILED bursts',
  });

  const runId = Date.now();
  const codes = [];
  const batchSize = 100;
  for (let batch = 0; batch * batchSize < COUPONS; batch++) {
    const requests = [];
    for (let i = 0; i < batchSize && batch * batchSize + i < COUPONS; i++) {
      const code = `PERFS2R${runId}X${batch * batchSize + i}`;
      codes.push(code);
      requests.push([
        'POST',
        `${BASE}/api/v1/coupons`,
        JSON.stringify({ code, maxUsageCount: 100000, countryCode: 'PL' }),
        { headers: HEADERS },
      ]);
    }
    http.batch(requests).forEach((r) => {
      if (r.status !== 201) {
        throw new Error(`setup: failed to create a coupon: ${r.status} ${r.body}`);
      }
    });
  }
  console.log(`S2: created ${codes.length} coupons (prefix PERFS2R${runId})`);
  return { codes };
}

export default function (data) {
  const code = data.codes[Math.floor(Math.random() * data.codes.length)];
  const res = http.post(
    `${BASE}/api/v1/coupons/redeem`,
    JSON.stringify({ code, userId: `u-${__VU}-${__ITER}` }),
    { headers: HEADERS, timeout: '10s' },
  );
  check(res, { 'status 201': (r) => r.status === 201 });
}
