// ============================================================
//  PRUEBA DE CARGA — endpoint PÚBLICO (sin login)
//  Objetivo: subir la CPU del api-gateway / course-service para
//  DISPARAR el auto scaling de ECS (1 -> 4 tareas) y verlo en CloudWatch.
//  Ejecutar:  k6 run load/carga-publica.js
// ============================================================
import http from 'k6/http';
import { check } from 'k6';

// Gateway por IP pública directa (sin ngrok = sin throttling).
// OJO: esta IP cambia si el gateway reinicia; si falla, pide la IP nueva.
const GATEWAY  = 'http://98.81.202.13:9000';
const ENDPOINT = '/actuator/health';   // local del gateway (rápido, sin Mongo) → estresa CPU del gateway

export const options = {
  stages: [
    { duration: '1m', target: 800 },    // rampa a 800 usuarios virtuales
    { duration: '10m', target: 800 },   // sostenido (muchos req/s → CPU del gateway sube)
    { duration: '1m', target: 0 },      // bajada
  ],
  thresholds: {
    http_req_failed: ['rate<0.5'],     // tolerante: la idea es estresar, no medir SLO
  },
};

export default function () {
  const res = http.get(`${GATEWAY}${ENDPOINT}`);
  check(res, { 'status 200': (r) => r.status === 200 });
}
