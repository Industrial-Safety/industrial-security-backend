puede// LT-02 DEMO — carga sostenida ~6 min con refresh de token por VU.
// Mantiene la CPU alta los 3+ min que el autoscaling necesita para escalar.
//
// k6 run -e GATEWAY_URL=... -e KEYCLOAK_URL=... -e CLIENT_ID=... -e CLIENT_SECRET=... \
//        -e TEST_USER=... -e TEST_PASS=... load/lt02-demo.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 40 },   // rampa
    { duration: '5m',  target: 40 },    // sostenido (CPU alta 5 min)
    { duration: '30s', target: 0 },     // bajada
  ],
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    http_req_failed:   ['rate<0.15'],   // tolerante: ngrok free throttlea (H8)
  },
};

const NGROK = { 'ngrok-skip-browser-warning': 'true' };

// Cada VU mantiene su propio token y lo renueva antes de que expire (~5 min).
let vuToken = null;
let vuTokenAt = 0;

function fetchToken() {
  const res = http.post(
    `${__ENV.KEYCLOAK_URL}/realms/industrial-safety/protocol/openid-connect/token`,
    {
      grant_type:    'password',
      client_id:     __ENV.CLIENT_ID,
      client_secret: __ENV.CLIENT_SECRET,
      username:      __ENV.TEST_USER,
      password:      __ENV.TEST_PASS,
    },
    { headers: NGROK },
  );
  check(res, { 'login 200': (r) => r.status === 200 });
  return res.json('access_token');
}

export default function () {
  const now = Date.now();
  if (!vuToken || now - vuTokenAt > 240000) {   // renueva cada 4 min
    vuToken = fetchToken();
    vuTokenAt = now;
  }
  const params = { headers: { Authorization: `Bearer ${vuToken}`, ...NGROK } };
  const res = http.get(`${__ENV.GATEWAY_URL}/api/v1/incidents`, params);
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(0.3);
}
