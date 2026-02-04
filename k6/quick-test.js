import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const loginDuration = new Trend('login_duration');
const activitiesDuration = new Trend('activities_duration');
const summaryDuration = new Trend('summary_duration');
const challengesDuration = new Trend('challenges_duration');
const plansDuration = new Trend('plans_duration');

// Quick test configuration (약 1분)
export const options = {
  stages: [
    { duration: '10s', target: 10 },   // Ramp up to 10 users
    { duration: '20s', target: 30 },   // Ramp up to 30 users
    { duration: '20s', target: 50 },   // Stay at 50 users
    { duration: '10s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95% of requests should be below 1000ms
    errors: ['rate<0.1'],               // Error rate should be below 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const TEST_USER = {
  email: `loadtest_${Date.now()}@test.com`,
  password: 'password123',
  nickname: '부하테스터'
};

export function setup() {
  const signupRes = http.post(`${BASE_URL}/api/auth/signup`, JSON.stringify({
    email: TEST_USER.email,
    password: TEST_USER.password,
    nickname: TEST_USER.nickname
  }), {
    headers: { 'Content-Type': 'application/json' }
  });

  if (signupRes.status === 200) {
    const body = JSON.parse(signupRes.body);
    return { token: body.accessToken, email: TEST_USER.email };
  }

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    email: TEST_USER.email,
    password: TEST_USER.password
  }), {
    headers: { 'Content-Type': 'application/json' }
  });

  if (loginRes.status === 200) {
    const body = JSON.parse(loginRes.body);
    return { token: body.accessToken, email: TEST_USER.email };
  }

  return { token: null, email: null };
}

export default function(data) {
  if (!data.token) {
    console.error('No auth token available');
    return;
  }

  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`
  };

  // 1. Login
  group('Login', function() {
    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      email: data.email,
      password: TEST_USER.password
    }), {
      headers: { 'Content-Type': 'application/json' }
    });
    loginDuration.add(Date.now() - start);
    errorRate.add(res.status !== 200);
  });

  sleep(0.3);

  // 2. Activities
  group('Activities', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/activities?page=0&size=10`, { headers: authHeaders });
    activitiesDuration.add(Date.now() - start);
    errorRate.add(res.status !== 200);
  });

  sleep(0.3);

  // 3. Summary
  group('Summary', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/activities/summary`, { headers: authHeaders });
    summaryDuration.add(Date.now() - start);
    errorRate.add(res.status !== 200);
  });

  sleep(0.3);

  // 4. Challenges
  group('Challenges', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/challenges`, { headers: authHeaders });
    challengesDuration.add(Date.now() - start);
    errorRate.add(res.status !== 200);
  });

  sleep(0.3);

  // 5. Plans
  group('Plans', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/plans`, { headers: authHeaders });
    plansDuration.add(Date.now() - start);
    errorRate.add(res.status !== 200);
  });

  sleep(0.5);
}

export function handleSummary(data) {
  const summary = {
    timestamp: new Date().toISOString(),
    phase: 'BASELINE',
    testDuration: `${(data.state.testRunDurationMs / 1000).toFixed(0)}s`,
    totalRequests: data.metrics.http_reqs?.values?.count || 0,
    maxVUs: data.metrics.vus_max?.values?.max || 0,
    responseTime: {
      avg: `${data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0}ms`,
      p95: `${data.metrics.http_req_duration?.values?.['p(95)']?.toFixed(2) || 0}ms`,
      max: `${data.metrics.http_req_duration?.values?.max?.toFixed(2) || 0}ms`,
    },
    errorRate: `${((data.metrics.errors?.values?.rate || 0) * 100).toFixed(2)}%`,
    throughput: `${(data.metrics.http_reqs?.values?.rate || 0).toFixed(2)} req/s`,
    endpoints: {
      login: `${data.metrics.login_duration?.values?.avg?.toFixed(2) || 0}ms`,
      activities: `${data.metrics.activities_duration?.values?.avg?.toFixed(2) || 0}ms`,
      summary: `${data.metrics.summary_duration?.values?.avg?.toFixed(2) || 0}ms`,
      challenges: `${data.metrics.challenges_duration?.values?.avg?.toFixed(2) || 0}ms`,
      plans: `${data.metrics.plans_duration?.values?.avg?.toFixed(2) || 0}ms`,
    }
  };

  const text = `
╔══════════════════════════════════════════════════════════════╗
║              LOAD TEST RESULTS - BASELINE                    ║
╠══════════════════════════════════════════════════════════════╣
║  Test Duration: ${summary.testDuration.padEnd(10)} │ Total Requests: ${String(summary.totalRequests).padEnd(8)} ║
║  Max VUs:       ${String(summary.maxVUs).padEnd(10)} │ Throughput:     ${summary.throughput.padEnd(8)} ║
╠══════════════════════════════════════════════════════════════╣
║  RESPONSE TIMES                                              ║
║    Average:  ${summary.responseTime.avg.padEnd(12)}                                 ║
║    P95:      ${summary.responseTime.p95.padEnd(12)}                                 ║
║    Max:      ${summary.responseTime.max.padEnd(12)}                                 ║
╠══════════════════════════════════════════════════════════════╣
║  ERROR RATE: ${summary.errorRate.padEnd(10)}                                       ║
╠══════════════════════════════════════════════════════════════╣
║  ENDPOINT RESPONSE TIMES (avg)                               ║
║    Login:      ${summary.endpoints.login.padEnd(10)}                                ║
║    Activities: ${summary.endpoints.activities.padEnd(10)}                                ║
║    Summary:    ${summary.endpoints.summary.padEnd(10)}                                ║
║    Challenges: ${summary.endpoints.challenges.padEnd(10)}                                ║
║    Plans:      ${summary.endpoints.plans.padEnd(10)}                                ║
╚══════════════════════════════════════════════════════════════╝
`;

  return {
    'stdout': text,
    'k6/baseline-result.json': JSON.stringify(summary, null, 2),
  };
}
