// ============================================================
//  PRUEBA DE CARGA (LOAD TEST) — todo hardcodeado, sin env vars
//  Ejecutar:  k6 run load/carga.js
//  Objetivo: simular carga realista sostenida y medir el SLO.
// ============================================================
import http from 'k6/http';
import { check, sleep } from 'k6';

// ---- CONFIG HARDCODEADA ----
// Gateway por IP publica DIRECTA (sin ngrok = sin throttling). OJO: esta IP
// cambia cada vez que el gateway reinicia; si falla, pide la IP nueva.
const GATEWAY  = 'http://44.214.44.221:9000';
const KEYCLOAK = 'http://industrial-safety.duckdns.org:8080';
const CLIENT_ID     = 'industrial-safety-client';
const CLIENT_SECRET = '<<PEGA_AQUI_EL_CLIENT_SECRET>>';  // <-- rellena antes de correr
const USER     = 'admin@industrialsafety.com';
const PASS     = '<<PEGA_AQUI_LA_PASSWORD>>';            // <-- rellena antes de correr
const ENDPOINT = '/api/v1/incidents';           // GET que devuelve 200 con rol ADMINISTRADOR
const NGROK = { 'ngrok-skip-browser-warning': 'true' };

export const options = {
  stages: [
    { duration: '1m',  target: 30 },   // rampa
    { duration: '3m',  target: 30 },   // carga sostenida
    { duration: '30s', target: 0 },    // bajada
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // SLO: p95 < 1s
    http_req_failed:   ['rate<0.10'],  // < 10% errores (ngrok free throttlea)
  },
};

// Cada VU mantiene su token y lo renueva antes de expirar (~5 min)
let token = null, tokenAt = 0;
function getToken() {
  const res = http.post(`${KEYCLOAK}/realms/industrial-safety/protocol/openid-connect/token`, {
    grant_type: 'password', client_id: CLIENT_ID, client_secret: CLIENT_SECRET,
    username: USER, password: PASS,
  }, { headers: NGROK });
  check(res, { 'login 200': (r) => r.status === 200 });
  return res.json('access_token');
}

export default function () {
  const now = Date.now();
  if (!token || now - tokenAt > 240000) { token = getToken(); tokenAt = now; }
  const res = http.get(`${GATEWAY}${ENDPOINT}`, {
    headers: { Authorization: `Bearer ${token}`, ...NGROK },
  });
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(0.5);
}
