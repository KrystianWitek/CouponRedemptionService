// S1 — single hot coupon: measures the row-serialization ceiling.
// Every request uses a unique user, so the only contention is the coupon row lock.
// The run is split into tagged scenarios so thresholds judge each plateau separately:
// `low` must stay green; `high` is expected to saturate — failed checks and dropped
// iterations there ARE the measurement (the capacity ceiling), not a test defect.
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:18080';
const RATE_LOW = Number(__ENV.RATE_LOW || 300);
const RATE_HIGH = Number(__ENV.RATE_HIGH || 3000);
const HOLD_S = toSeconds(__ENV.HOLD || '2m');
const WARMUP_S = 30;
const RAMP_S = 30;
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
    // The healthy plateau must meet the SLO and sustain the offered rate; the warmup
    // and the deliberately saturating `high` scenario stay outside these thresholds.
    'http_req_duration{scenario:low,expected_response:true}': ['p(95)<200', 'p(99)<500'],
    'checks{scenario:low}': ['rate>0.999'],
    'dropped_iterations{scenario:low}': ['count<100'],
  },
};

export function setup() {
  const code = `PERFS1${Date.now()}`;
  const res = http.post(
    `${BASE}/api/v1/coupons`,
    JSON.stringify({ code, maxUsageCount: 2000000, countryCode: 'PL' }),
    { headers: HEADERS },
  );
  if (res.status !== 201) {
    throw new Error(`setup: failed to create the coupon: ${res.status} ${res.body}`);
  }
  console.log(`S1 coupon code: ${code}`);
  return { code };
}

export default function (data) {
  const res = http.post(
    `${BASE}/api/v1/coupons/redeem`,
    JSON.stringify({ code: data.code, userId: `u-${__VU}-${__ITER}` }),
    { headers: HEADERS, timeout: '10s' },
  );
  check(res, { 'status 201': (r) => r.status === 201 });
}
