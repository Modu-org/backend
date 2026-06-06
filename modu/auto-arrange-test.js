import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 1,
  iterations: 1,
};

const BASE_URL = 'http://localhost:8080';

// 여기를 네가 테스트할 일정 ID로 바꿔야 함
const SCHEDULE_ID = 6;

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({
      userName: 'testuser1',
      password: 'password123!',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );

  check(loginRes, {
    'login status is 200': (r) => r.status === 200,
  });

  const body = JSON.parse(loginRes.body);

  return {
    token: body.data.accessToken,
  };
}

export default function (data) {
  const payload = JSON.stringify({
    days: [
      {
        date: '2026-06-07',
        startNodeId: null,
        endNodeId: null,
      },
      {
        date: '2026-06-08',
        startNodeId: null,
        endNodeId: null,
      },
      {
        date: '2026-06-09',
        startNodeId: null,
        endNodeId: null,
      },
    ],
  });

  const res = http.post(
    `${BASE_URL}/api/schedules/${SCHEDULE_ID}/auto-arrange`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${data.token}`,
      },
      timeout: '120s',
    }
  );

  check(res, {
    'auto arrange status is 200': (r) => r.status === 200,
  });

  console.log(`auto-arrange status=${res.status}`);
  console.log(`auto-arrange duration=${res.timings.duration}ms`);

  if (res.status !== 200) {
    console.log(res.body);
  }
}