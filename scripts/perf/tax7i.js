// 7iTAX BE 부하 테스트 (k6). 시나리오·합격 기준·실행 방법은 docs/PERF_TEST.md
//
//   PROFILE   smoke | load | stress | spike | soak          (기본 smoke)
//   BASE_URL  대상 BE (기본 http://backend:8080 — compose 네트워크 안에서 실행할 때)
//   USERS     테스트 사용자 수 (기본 50). 전화번호 PHONE_PREFIX + 4자리로 간편 로그인
//   VUS       load·soak 목표 동시 사용자 (기본 50 / 30)
//   DURATION  soak 유지 시간 (기본 30m)
//   CONTENTION=1  모든 VU가 사용자 1명(계좌 1개)으로 결제 → 계좌 행 잠금 경합 측정
//
// 전제: 대상 BE가 TEST_LOGIN_ENABLED=true (간편 로그인으로 사용자·계좌·카드를 만든다). 운영 실서비스에는 돌리지 않는다
import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://backend:8080').replace(/\/$/, '');
const PROFILE = __ENV.PROFILE || 'smoke';
const USERS = parseInt(__ENV.USERS || '50', 10);
const PHONE_PREFIX = __ENV.PHONE_PREFIX || '0107700';
const CONTENTION = __ENV.CONTENTION === '1';
const YEAR = new Date().getFullYear();

const ledgerMissing = new Counter('ledger_missing'); // 결제 확정 후 장부 미생성(비동기 리스너 지연·실패)

// ───────────── 부하 프로필 ─────────────
const VUS = parseInt(__ENV.VUS || (PROFILE === 'soak' ? '30' : '50'), 10);
const PROFILES = {
  // 스크립트·환경이 정상인지만 확인
  smoke: { executor: 'constant-vus', vus: 2, duration: '30s' },
  // 평상시 부하: 목표 동시 사용자에서 응답 시간·오류율이 기준 안에 드는지
  load: {
    executor: 'ramping-vus', startVUs: 0,
    stages: [{ duration: '1m', target: VUS }, { duration: '5m', target: VUS }, { duration: '30s', target: 0 }],
  },
  // 한계 탐색: 단계적으로 올려 기준이 깨지는 지점(처리량 정체·p95 급증·오류)을 찾는다
  stress: {
    executor: 'ramping-vus', startVUs: 0,
    stages: [
      { duration: '1m', target: 50 }, { duration: '2m', target: 50 },
      { duration: '1m', target: 100 }, { duration: '2m', target: 100 },
      { duration: '1m', target: 200 }, { duration: '2m', target: 200 },
      { duration: '1m', target: 300 }, { duration: '2m', target: 300 },
      { duration: '1m', target: 0 },
    ],
  },
  // 급증: 평상시 → 순간 폭증 → 복귀. 폭증 중 오류와 복귀 후 회복을 본다
  spike: {
    executor: 'ramping-vus', startVUs: 0,
    stages: [
      { duration: '30s', target: 10 }, { duration: '10s', target: 200 }, { duration: '1m', target: 200 },
      { duration: '10s', target: 10 }, { duration: '1m', target: 10 }, { duration: '10s', target: 0 },
    ],
  },
  // 지속: 같은 부하를 오래 유지해 메모리 누수·커넥션 누수·점진적 지연을 본다
  soak: {
    executor: 'ramping-vus', startVUs: 0,
    stages: [{ duration: '1m', target: VUS }, { duration: __ENV.DURATION || '30m', target: VUS }, { duration: '30s', target: 0 }],
  },
};
if (!PROFILES[PROFILE]) fail(`알 수 없는 PROFILE=${PROFILE}`);

// ───────────── 합격 기준 (docs/PERF_TEST.md 2절) ─────────────
// stress·spike는 한계를 찾는 테스트라 기준을 넘어도 실행을 멈추지 않는다(결과만 기록)
export const options = {
  scenarios: { main: PROFILES[PROFILE] },
  setupTimeout: '5m',
  thresholds: {
    'http_req_failed{flow:read}': ['rate<0.01'],
    'http_req_failed{flow:pay}': ['rate<0.01'],
    'http_req_duration{flow:read}': ['p(95)<500'],
    'http_req_duration{flow:pay}': ['p(95)<1000'],
    'http_req_duration{flow:export}': ['p(95)<3000'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// ───────────── 공통 ─────────────
function phoneOf(i) {
  return PHONE_PREFIX + String(i).padStart(4, '0');
}

function login(phone) {
  const res = http.post(`${BASE_URL}/api/auth/simple-login`,
    JSON.stringify({ phoneNumber: phone, name: `부하${phone.slice(-4)}` }),
    { headers: { 'Content-Type': 'application/json' }, tags: { flow: 'auth' } });
  if (res.status !== 200) fail(`간편 로그인 실패 ${phone}: HTTP ${res.status} ${res.body}`);
  return res.json('data.accessToken');
}

function auth(token, flow) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, tags: { flow } };
}

// 사용자·계좌·카드는 측정 전에 한 번만 만든다(처음 로그인하는 번호는 계좌·카드 발급까지 해서 느리다)
export function setup() {
  const count = CONTENTION ? 1 : USERS;
  const users = [];
  for (let i = 1; i <= count; i++) {
    const phone = phoneOf(i);
    const token = login(phone);
    const cards = http.get(`${BASE_URL}/api/cards`, auth(token, 'auth')).json('data');
    const card = cards.find((c) => c.cardType === 'BUSINESS');
    if (!card) fail(`${phone}: 사업자 카드 없음`);
    users.push({ phone, cardId: card.id });
  }
  const anyToken = login(phoneOf(1));
  const merchants = http.get(`${BASE_URL}/api/cards/merchants`, auth(anyToken, 'auth')).json('data');
  return { users, merchants };
}

// VU별 토큰 캐시. 액세스 토큰 만료(30분)·로그아웃으로 403이 나면 다시 로그인한다
let me = null;

function ensureLogin(data) {
  if (!me) {
    const u = data.users[(__VU - 1) % data.users.length];
    me = { ...u, token: login(u.phone) };
  }
  return me;
}

function call(method, url, body, flow) {
  let res = http.request(method, url, body ? JSON.stringify(body) : null, auth(me.token, flow));
  if (res.status === 401 || res.status === 403) {
    me.token = login(me.phone);
    res = http.request(method, url, body ? JSON.stringify(body) : null, auth(me.token, flow));
  }
  return res;
}

// ───────────── 사용자 행동 ─────────────
function readFlow() {
  const urls = [
    `${BASE_URL}/api/book-entries?size=20`,
    `${BASE_URL}/api/book-entries/summary?year=${YEAR}`,
    `${BASE_URL}/api/book-entries/unconfirmed-count`,
    `${BASE_URL}/api/tax-estimation?year=${YEAR}`,
    `${BASE_URL}/api/banking/accounts`,
    `${BASE_URL}/api/cards`,
  ];
  const res = call('GET', urls[Math.floor(Math.random() * urls.length)], null, 'read');
  check(res, { 'read 200': (r) => r.status === 200 });
}

function payFlow(data, cancel) {
  const m = data.merchants[Math.floor(Math.random() * data.merchants.length)];
  const amount = 1000 + Math.floor(Math.random() * 9000);
  const a = call('POST', `${BASE_URL}/api/payments/authorize`, {
    cardId: me.cardId, amount, merchantId: m.merchantId, merchantName: m.merchantName,
    paymentMethod: 'OFFLINE', purpose: 'BUSINESS',
  }, 'pay');
  if (!check(a, { 'authorize 200': (r) => r.status === 200 })) return;
  const paymentId = a.json('data.paymentId');

  const c = call('POST', `${BASE_URL}/api/payments/${paymentId}/capture`, null, 'pay');
  check(c, { 'capture CAPTURED': (r) => r.status === 200 && r.json('data.status') === 'CAPTURED' });

  if (cancel) {
    const x = call('POST', `${BASE_URL}/api/payments/${paymentId}/cancel`, { reason: '부하 테스트 취소' }, 'pay');
    check(x, { 'cancel 200': (r) => r.status === 200 });
  }
}

function exportFlow() {
  const res = call('GET', `${BASE_URL}/api/export/simple-ledger/pdf?year=${YEAR}`, null, 'export');
  check(res, { 'export pdf': (r) => r.status === 200 && r.body && r.body.length > 0 });
}

// 장부 자동 생성(비동기)이 따라오는지 표본 확인. 응답 시간 기준에는 넣지 않는다(flow:verify)
function ledgerSample() {
  const res = call('GET', `${BASE_URL}/api/payments?size=1`, null, 'verify');
  const latest = res.status === 200 ? res.json('data.content.0') : null;
  if (!latest || latest.status !== 'CAPTURED') return;
  sleep(2);
  const page = call('GET', `${BASE_URL}/api/book-entries?size=50`, null, 'verify');
  const found = page.status === 200 && (page.json('data.content') || []).some((e) => e.paymentId === latest.paymentId);
  if (!found) ledgerMissing.add(1);
}

// 행동 비율: 조회 65% / 결제 25% / 결제 후 취소 5% / PDF 내보내기 4% / 장부 생성 확인 1%
// CONTENTION=1이면 결제만 한다(한 계좌에 동시 결제)
export default function (data) {
  ensureLogin(data);
  if (CONTENTION) {
    payFlow(data, false);
  } else {
    const r = Math.random();
    if (r < 0.65) readFlow();
    else if (r < 0.90) payFlow(data, false);
    else if (r < 0.95) payFlow(data, true);
    else if (r < 0.99) exportFlow();
    else ledgerSample();
  }
  sleep(0.5 + Math.random()); // 사람의 생각 시간 0.5~1.5초
}
