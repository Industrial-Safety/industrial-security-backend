# Simulación: carga-publica.js

## ¿Qué hace?

Prueba de carga con **k6** que golpea `/actuator/health` del API Gateway (`http://98.81.202.13:9000`) para **subir la CPU** y disparar el auto-scaling de ECS (1 → 4 tareas).

## Perfil de carga

| Etapa | Duración | VUs |
|-------|----------|-----|
| Rampa | 1 min | 0 → 800 |
| Sostenido | 10 min | 800 |
| Bajada | 1 min | 800 → 0 |

## Salida típica de k6

```
     ✓ status 200

     checks.........................: 100.00% ✓ 480000  ✗ 0
     data_received..................: 280 MB  ~450 KB/s
     data_sent......................: 48 MB   ~78 KB/s
     http_req_blocked...............: avg=1ms    p95=2ms
     http_req_connecting............: avg=0.5ms  p95=1ms
     http_req_duration..............: avg=120ms  p95=350ms   p99=500ms
     http_req_failed................: 0.01%   ✓ 48     ✗ 479952
     http_req_receiving.............: avg=0.1ms  p95=0.2ms
     http_req_sending...............: avg=0.1ms  p95=0.2ms
     http_req_tls_handshaking.......: avg=0ms    p95=0ms
     http_req_waiting...............: avg=119ms  p95=349ms
     http_requests..................: 480000  ~780 req/s
     vus...........................: 800     min=0   max=800
     vus_max.......................: 800     min=0   max=800
```

## Cómo interpretar

| Métrica | Qué significa |
|---------|---------------|
| `status 200` | El gateway responde OK |
| `http_req_duration` p95 | Latencia. Si sube durante el test → CPU saturada → auto-scaling debería dispararse |
| `http_req_failed` | < 1% = gateway aguanta. > 50% = colapsó antes de escalar |
| `threshold: rate<0.5` | Tolerante a propósito (el objetivo es estresar, no medir SLO) |

## Señal de auto-scaling funcionando (CloudWatch)

1. `CPUUtilization` del cluster ECS sube > 80%
2. CloudWatch Alarm se dispara
3. `DesiredCount` del servicio pasa 1 → 2 → 4 tareas
4. La latencia (p95) se estabiliza o baja cuando entran las nuevas tareas

## Resumen

800 VUs durante 10 min contra `/actuator/health` para forzar el escalado horizontal de ECS. k6 reporta éxito/fallo/latencia para evaluar si el auto-scaling responde a tiempo.
