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

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up to 10 users
    { duration: '1m', target: 50 },    // Ramp up to 50 users
    { duration: '30s', target: 100 },  // Ramp up to 100 users
    { duration: '1m', target: 100 },   // Stay at 100 users
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests should be below 500ms
    errors: ['rate<0.1'],               // Error rate should be below 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test user credentials (will be created during setup)
const TEST_USER = {
  email: `loadtest_${Date.now()}@test.com`,
  password: 'password123',
  nickname: '부하테스터'
};

let authToken = null;

export function setup() {
  // Create test user
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

  // If signup fails (user exists), try login
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

  console.error('Setup failed: Could not create or login test user');
  return { token: null, email: null };
}

export default function(data) {
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`
  };

  // 1. Login test
  group('Login', function() {
    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      email: data.email,
      password: TEST_USER.password
    }), {
      headers: { 'Content-Type': 'application/json' }
    });
    loginDuration.add(Date.now() - start);

    const success = check(res, {
      'login status is 200': (r) => r.status === 200,
      'login has token': (r) => JSON.parse(r.body).accessToken !== undefined,
    });
    errorRate.add(!success);
  });

  sleep(0.5);

  // 2. Get activities list
  group('Get Activities', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/activities?page=0&size=10`, {
      headers: authHeaders
    });
    activitiesDuration.add(Date.now() - start);

    const success = check(res, {
      'activities status is 200': (r) => r.status === 200,
      'activities has content': (r) => JSON.parse(r.body).content !== undefined,
    });
    errorRate.add(!success);
  });

  sleep(0.5);

  // 3. Get activity summary
  group('Get Summary', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/activities/summary`, {
      headers: authHeaders
    });
    summaryDuration.add(Date.now() - start);

    const success = check(res, {
      'summary status is 200': (r) => r.status === 200,
      'summary has thisWeek': (r) => JSON.parse(r.body).thisWeek !== undefined,
    });
    errorRate.add(!success);
  });

  sleep(0.5);

  // 4. Get challenges
  group('Get Challenges', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/challenges`, {
      headers: authHeaders
    });
    challengesDuration.add(Date.now() - start);

    const success = check(res, {
      'challenges status is 200': (r) => r.status === 200,
      'challenges is array': (r) => Array.isArray(JSON.parse(r.body)),
    });
    errorRate.add(!success);
  });

  sleep(0.5);

  // 5. Get plans
  group('Get Plans', function() {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/plans`, {
      headers: authHeaders
    });
    plansDuration.add(Date.now() - start);

    const success = check(res, {
      'plans status is 200': (r) => r.status === 200,
      'plans is array': (r) => Array.isArray(JSON.parse(r.body)),
    });
    errorRate.add(!success);
  });

  sleep(1);
}

export function handleSummary(data) {
  const summary = {
    timestamp: new Date().toISOString(),
    testDuration: data.state.testRunDurationMs,
    metrics: {
      http_reqs: data.metrics.http_reqs?.values?.count || 0,
      http_req_duration_avg: data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0,
      http_req_duration_p95: data.metrics.http_req_duration?.values?.['p(95)']?.toFixed(2) || 0,
      http_req_duration_max: data.metrics.http_req_duration?.values?.max?.toFixed(2) || 0,
      error_rate: ((data.metrics.errors?.values?.rate || 0) * 100).toFixed(2) + '%',
      vus_max: data.metrics.vus_max?.values?.max || 0,
    },
    endpoints: {
      login: data.metrics.login_duration?.values?.avg?.toFixed(2) || 0,
      activities: data.metrics.activities_duration?.values?.avg?.toFixed(2) || 0,
      summary: data.metrics.summary_duration?.values?.avg?.toFixed(2) || 0,
      challenges: data.metrics.challenges_duration?.values?.avg?.toFixed(2) || 0,
      plans: data.metrics.plans_duration?.values?.avg?.toFixed(2) || 0,
    }
  };

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'k6/baseline-result.json': JSON.stringify(summary, null, 2),
  };
}

function textSummary(data, opts) {
  const lines = [];
  lines.push('\n========================================');
  lines.push('       LOAD TEST RESULTS (BASELINE)');
  lines.push('========================================\n');

  lines.push(`Total Requests: ${data.metrics.http_reqs?.values?.count || 0}`);
  lines.push(`Max VUs: ${data.metrics.vus_max?.values?.max || 0}`);
  lines.push(`Test Duration: ${(data.state.testRunDurationMs / 1000).toFixed(0)}s\n`);

  lines.push('Response Times:');
  lines.push(`  Average: ${data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0}ms`);
  lines.push(`  P95: ${data.metrics.http_req_duration?.values?.['p(95)']?.toFixed(2) || 0}ms`);
  lines.push(`  Max: ${data.metrics.http_req_duration?.values?.max?.toFixed(2) || 0}ms\n`);

  lines.push('Error Rate: ' + ((data.metrics.errors?.values?.rate || 0) * 100).toFixed(2) + '%\n');

  lines.push('Endpoint Avg Response Times:');
  lines.push(`  Login:      ${data.metrics.login_duration?.values?.avg?.toFixed(2) || 0}ms`);
  lines.push(`  Activities: ${data.metrics.activities_duration?.values?.avg?.toFixed(2) || 0}ms`);
  lines.push(`  Summary:    ${data.metrics.summary_duration?.values?.avg?.toFixed(2) || 0}ms`);
  lines.push(`  Challenges: ${data.metrics.challenges_duration?.values?.avg?.toFixed(2) || 0}ms`);
  lines.push(`  Plans:      ${data.metrics.plans_duration?.values?.avg?.toFixed(2) || 0}ms`);

  lines.push('\n========================================\n');

  return lines.join('\n');
}
