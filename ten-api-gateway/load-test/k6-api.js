import crypto from 'k6/crypto';
import http from 'k6/http';
import { check } from 'k6';

const accessKey = __ENV.ACCESS_KEY;
const secretKey = __ENV.SECRET_KEY;
const gatewayHost = __ENV.GATEWAY_HOST || 'http://localhost:8090';

export const options = {
  scenarios: {
    signed_api: {
      executor: 'per-vu-iterations',
      vus: Number(__ENV.VUS || 10),
      iterations: Number(__ENV.ITERATIONS || 5),
      maxDuration: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export function setup() {
  if (!accessKey || !secretKey) {
    throw new Error('ACCESS_KEY and SECRET_KEY are required');
  }
}

export default function () {
  const body = JSON.stringify({ username: `load-user-${__VU}` });
  const nonceSeed = `${Date.now()}-${__VU}-${__ITER}-${Math.random()}`;
  const nonce = crypto.sha256(nonceSeed, 'hex').slice(0, 24);
  const sign = crypto.sha256(`${body}.${secretKey}`, 'hex');

  const response = http.post(`${gatewayHost}/api/name/object`, body, {
    headers: {
      'Content-Type': 'application/json',
      accessKey,
      nonce,
      timestamp: String(Math.floor(Date.now() / 1000)),
      sign,
      body,
    },
  });

  check(response, {
    'status is 200': (result) => result.status === 200,
  });
}
