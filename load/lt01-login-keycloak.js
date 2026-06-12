// LT-01 — Login masivo contra Keycloak (Plan §3.2)
// Simula una campaña: usuarios autenticándose a la vez. El hashing de password
// (PBKDF2) es CPU-bound, así que esta prueba estresa la CPU de Keycloak.
//
// Ejecutar:
//   k6 run -e KEYCLOAK_URL=https://tu-keycloak -e CLIENT_ID=web \
//          -e TEST_USER=demo -e TEST_PASS=demo load/lt01-login-keycloak.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '5m',  target: 50 },   // rampa 0 -> 50 logins/s
    { duration: '10m', target: 50 },   // sostenido
    { duration: '2m',  target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // p95 emisión de token < 1 s
    http_req_failed:   ['rate<0.01'],
  },
};

export default function () {
  const res = http.post(
    `${__ENV.KEYCLOAK_URL}/realms/industrial-safety/protocol/openid-connect/token`,
    {
      grant_type: 'password',
      client_id:  __ENV.CLIENT_ID,
      username:   __ENV.TEST_USER,
      password:   __ENV.TEST_PASS,
    },
  );
  check(res, {
    'token 200':       (r) => r.status === 200,
    'tiene token':     (r) => r.json('access_token') !== undefined,
  });
}
