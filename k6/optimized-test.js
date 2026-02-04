/**
 * 최적화 후 성능 측정 스크립트
 *
 * baseline-result.json과 비교하여 개선율 측정
 *
 * 실행:
 *   k6 run k6/optimized-test.js
 *   k6 run k6/optimized-test.js --env BASE_URL=https://example.com
 */

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

// 베이스라인과 동일한 테스트 설정
export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const TEST_USER = {
  email: `opttest_${Date.now()}@test.com`,
  password: 'password123',
  nickname: '최적화테스터'
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
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`
  };

  group('Login', function() {
    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      email: data.email,
      password: TEST_USER.password
    }), {
      headers: { 'Content-Type': 'application/json' }
    });
    loginDuration.add(Date.now() - start);
    errorRate.add(!check(res, { 'login ok': (r) => r.status === 200 }));
  });

  sleep(0.5);

  group('Get Activities', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/activities?page=0&size=10`, { headers: authHeaders });
    activitiesDuration.add(Date.now() - start);
    errorRate.add(!check(res, { 'activities ok': (r) => r.status === 200 }));
  });

  sleep(0.5);

  group('Get Summary', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/activities/summary`, { headers: authHeaders });
    summaryDuration.add(Date.now() - start);
    errorRate.add(!check(res, { 'summary ok': (r) => r.status === 200 }));
  });

  sleep(0.5);

  group('Get Challenges', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/challenges`, { headers: authHeaders });
    challengesDuration.add(Date.now() - start);
    errorRate.add(!check(res, { 'challenges ok': (r) => r.status === 200 }));
  });

  sleep(0.5);

  group('Get Plans', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/plans`, { headers: authHeaders });
    plansDuration.add(Date.now() - start);
    errorRate.add(!check(res, { 'plans ok': (r) => r.status === 200 }));
  });

  sleep(1);
}

export function handleSummary(data) {
  const result = {
    timestamp: new Date().toISOString(),
    phase: "OPTIMIZED",
    testDuration: `${(data.state.testRunDurationMs / 1000).toFixed(0)}s`,
    totalRequests: data.metrics.http_reqs?.values?.count || 0,
    maxVUs: data.metrics.vus_max?.values?.max || 0,
    responseTime: {
      avg: `${data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0}ms`,
      p95: `${data.metrics.http_req_duration?.values?.['p(95)']?.toFixed(2) || 0}ms`,
      max: `${data.metrics.http_req_duration?.values?.max?.toFixed(2) || 0}ms`,
    },
    errorRate: `${((data.metrics.errors?.values?.rate || 0) * 100).toFixed(2)}%`,
    throughput: `${(data.metrics.http_reqs?.values?.count / (data.state.testRunDurationMs / 1000)).toFixed(2)} req/s`,
    endpoints: {
      login: `${data.metrics.login_duration?.values?.avg?.toFixed(2) || 0}ms`,
      activities: `${data.metrics.activities_duration?.values?.avg?.toFixed(2) || 0}ms`,
      summary: `${data.metrics.summary_duration?.values?.avg?.toFixed(2) || 0}ms`,
      challenges: `${data.metrics.challenges_duration?.values?.avg?.toFixed(2) || 0}ms`,
      plans: `${data.metrics.plans_duration?.values?.avg?.toFixed(2) || 0}ms`,
    }
  };

  // 콘솔 출력
  console.log('\n==========================================');
  console.log('    OPTIMIZED PERFORMANCE RESULTS');
  console.log('==========================================');
  console.log(`Total Requests: ${result.totalRequests}`);
  console.log(`Throughput: ${result.throughput}`);
  console.log(`Max VUs: ${result.maxVUs}`);
  console.log(`\nResponse Time:`);
  console.log(`  Average: ${result.responseTime.avg}`);
  console.log(`  P95: ${result.responseTime.p95}`);
  console.log(`  Max: ${result.responseTime.max}`);
  console.log(`\nError Rate: ${result.errorRate}`);
  console.log(`\nEndpoint Avg Response Times:`);
  console.log(`  Login:      ${result.endpoints.login}`);
  console.log(`  Activities: ${result.endpoints.activities}`);
  console.log(`  Summary:    ${result.endpoints.summary}`);
  console.log(`  Challenges: ${result.endpoints.challenges}`);
  console.log(`  Plans:      ${result.endpoints.plans}`);
  console.log('==========================================\n');

  return {
    'k6/optimized-result.json': JSON.stringify(result, null, 2),
  };
}
