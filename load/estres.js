// ============================================================
//  PRUEBA DE ESTRES (STRESS TEST) — todo hardcodeado, sin env vars
//  Ejecutar:  k6 run load/estres.js
//  Objetivo: subir la carga por escalones hasta el punto de quiebre
//            (ver cuando la latencia se dispara o aparecen errores).
// ============================================================
import http from 'k6/http';
import { check, sleep } from 'k6';

// ---- CONFIG HARDCODEADA ----
// Gateway por IP publica DIRECTA (sin ngrok = sin throttling, estres real).
// OJO: esta IP cambia cada vez que el gateway reinicia; si falla, pide la IP nueva.
const GATEWAY  = 'http://44.214.44.221:9000';
const KEYCLOAK = 'http://industrial-safety.duckdns.org:8080';
const CLIENT_ID     = 'industrial-safety-client';
const CLIENT_SECRET = '<<PEGA_AQUI_EL_CLIENT_SECRET>>';  // <-- rellena antes de correr
const USER     = 'admin@industrialsafety.com';
const PASS     = '<<PEGA_AQUI_LA_PASSWORD>>';            // <-- rellena antes de correr
const ENDPOINT = '/api/v1/incidents';
const NGROK = { 'ngrok-skip-browser-warning': 'true' };

export const options = {
  // Escalones crecientes hasta romper. Sin thresholds estrictos:
  // el objetivo es OBSERVAR donde se degrada, no pasar/fallar.
  stages: [
    { duration: '1m', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '1m', target: 200 },
    { duration: '1m', target: 300 },
    { duration: '1m', target: 400 },
    { duration: '1m', target: 0 },     // recuperacion
  ],
};

let token = null, tokenAt = 0;
function getToken() {
  const res = http.post(`${KEYCLOAK}/realms/industrial-safety/protocol/openid-connect/token`, {
    grant_type: 'password', client_id: CLIENT_ID, client_secret: CLIENT_SECRET,
    username: USER, password: PASS,
  }, { headers: NGROK });
  return res.json('access_token');
}

export default function () {
  const now = Date.now();
  if (!token || now - tokenAt > 240000) { token = getToken(); tokenAt = now; }
  const res = http.get(`${GATEWAY}${ENDPOINT}`, {
    headers: { Authorization: `Bearer ${token}`, ...NGROK },
  });
  check(res, {
    'status 200': (r) => r.status === 200,
    'rapido (<1s)': (r) => r.timings.duration < 1000,
  });
  sleep(0.3);
}
