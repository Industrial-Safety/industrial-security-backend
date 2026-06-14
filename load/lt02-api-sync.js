// LT-02 — Carga síncrona de APIs vía api-gateway (Plan §3.2)
// Simula el crecimiento ×10: rampa de 8 a 83 req/s atravesando el gateway con JWT real.
//
// Ejecutar:
//   k6 run -e GATEWAY_URL=https://tu-gateway -e KEYCLOAK_URL=https://tu-keycloak \
//          -e CLIENT_ID=web -e TEST_USER=demo -e TEST_PASS=demo load/lt02-api-sync.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5m',  target: 50 },   // rampa
    { duration: '15m', target: 50 },   // sostenido (~83 req/s con el sleep)
    { duration: '2m',  target: 0 },    // bajada
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // SLO: p95 < 500 ms  -> si falla, exit code != 0
    http_req_failed:   ['rate<0.01'],  // < 1% de errores
  },
};

// Header necesario para saltar la pagina de advertencia de ngrok (plan gratuito).
const NGROK = { 'ngrok-skip-browser-warning': 'true' };

// setup(): obtiene UNA vez el token de Keycloak y lo comparte a todos los VUs
export function setup() {
  const res = http.post(
    `${__ENV.KEYCLOAK_URL}/realms/industrial-safety/protocol/openid-connect/token`,
    {
      grant_type:    'password',
      client_id:     __ENV.CLIENT_ID,
      client_secret: __ENV.CLIENT_SECRET,   // client confidencial
      username:      __ENV.TEST_USER,
      password:      __ENV.TEST_PASS,
    },
    { headers: NGROK },
  );
  check(res, { 'login 200': (r) => r.status === 200 });
  return { token: res.json('access_token') };
}

export default function (data) {
  const params = { headers: { Authorization: `Bearer ${data.token}`, ...NGROK } };
  const res = http.get(`${__ENV.GATEWAY_URL}/api/v1/incidents`, params);
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(0.6);  // ritmo por VU -> ~83 req/s con 50 VUs
}
