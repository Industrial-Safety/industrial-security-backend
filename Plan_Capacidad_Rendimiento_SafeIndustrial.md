# Plan Integral de Capacidad y Rendimiento — SafeIndustrial Backend (AWS)

**Proyecto:** Plataforma de Gestión de Seguridad Industrial (SafeIndustrial)
**Fecha:** 2026-06-10 · **Relevamiento real de la cuenta AWS:** 2026-06-11
**Audiencia:** Equipo DevOps y desarrolladores backend
**Marco de referencia:** AWS Well-Architected Framework (pilares de Eficiencia de Rendimiento y Fiabilidad) + prácticas ITIL de Gestión de Capacidad (análisis → diagnóstico → planificación → KPIs → herramientas)

---

## 1. Introducción y contexto técnico

### 1.1 Inventario real de la arquitectura

Este plan **no es genérico**: está construido sobre el código y la configuración real del repositorio. Inventario verificado:

| Componente | Tecnología real | Evidencia en el repo |
|---|---|---|
| Lenguaje/Framework | Java 25 (Temurin JRE Alpine), Spring Boot 4.0.6, **virtual threads habilitados** | `safety-service/src/main/resources/application.yml` (`spring.threads.virtual.enabled: true`), Dockerfiles `eclipse-temurin:25-jre-alpine` |
| API Gateway | **Spring Cloud Gateway propio** (puerto 9000), validación JWT como Resource Server + TokenRelay | `api-gateway/src/main/resources/application.yaml` |
| Autenticación | Keycloak 24.0.1, realm `industrial-safety`, issuer `http://industrial-safety.duckdns.org:8080` | `api-gateway/application.yaml`, `docker-compose.yml` |
| Mensajería | RabbitMQ 3.13 → **Amazon MQ** en producción. **8 servicios** con AMQP: purchase, safety, exam, solicitudes, payment, order, notification, course | `pom.xml` de cada servicio (`spring-boot-starter-amqp`) |
| Colas reales | Durables + DLQ ya configuradas: `SOLICITUDES_QUEUE`, `ORDER_CREATED_QUEUE`, `PAYMENT_RESULT_QUEUE`, `EMAIL_QUEUE`, `CERT_QUEUE`, `WS_ALERT_QUEUE` (+ DLQs) | Clases `RabbitMQConfig` de cada servicio |
| BD relacional | PostgreSQL 16 por servicio (user, order, exam, payment, purchase, safety, keycloak) → **AWS RDS** | `docker-compose.yml` |
| BD documental | MongoDB 7 (course-service, chat-service) — **equivalente productivo por confirmar** (ver §1.4) | `docker-compose.yml` |
| Caché | Redis 7 (course-service) | `docker-compose.yml`, `course-service/pom.xml` |
| Configuración | **AWS Parameter Store** (`spring.config.import: aws-parameterstore:...`), región `us-east-1` | `application.yml` de los servicios |
| Descubrimiento | Local: Eureka. **Producción: AWS Cloud Map (DNS)** — no se mezclan | Convención del proyecto |
| Despliegue | ECR + ECS, 11 servicios en producción vía GitHub Actions (OIDC, `force-new-deployment`) | `.github/workflows/deploy.yml` (nombres ECS reales, p. ej. `safety-service-service-6r85geni`) |
| Calidad | JUnit + Mockito + Testcontainers, **gate JaCoCo 80%** en CI | `.github/workflows/ci.yml` |
| Observabilidad | CloudWatch por defecto; Actuator/Micrometer ya presente en 10 servicios | `pom.xml` (actuator), `management.endpoints` en api-gateway |

### 1.2 Diagrama lógico (texto)

```
                         Internet
                            │
                  ┌─────────▼──────────┐        ┌──────────────────┐
                  │  ALB / NLB (AWS)   │        │ Keycloak 24 (ECS) │
                  └─────────┬──────────┘        │ realm:            │
                            │                   │ industrial-safety │
                  ┌─────────▼──────────┐  JWKS  └────────┬─────────┘
                  │ api-gateway (SCG)  │◄────────────────┘
                  │ JWT validation +   │            ┌────────────┐
                  │ X-User-Id ← sub    │            │ keycloak-db │ (RDS PG)
                  └─────────┬──────────┘            └────────────┘
              Cloud Map DNS │ (namespace privado)
   ┌──────────┬─────────┬───┴──────┬──────────┬───────────┐
   ▼          ▼         ▼          ▼          ▼           ▼
 user      safety   purchase  solicitudes  course      exam ...
 service   service   service    service    service    service   (ECS Fargate)
   │          │         │          │          │           │
   ▼          ▼         ▼          ▼          ▼           ▼
 RDS PG    RDS PG    RDS PG     RDS PG    MongoDB+Redis  RDS PG + S3
   
        Amazon MQ (RabbitMQ) ──── colas durables + DLQ
   order ──ORDER_CREATED──► payment ──PAYMENT_RESULT──► order
   purchase/safety/exam/solicitudes ──eventos──► notification
   notification: EMAIL_QUEUE / CERT_QUEUE / WS_ALERT_QUEUE
```

### 1.3 Supuestos y objetivos de nivel de servicio (SLO)

| Parámetro | Valor actual (supuesto) | Objetivo (campaña, +3 meses) |
|---|---|---|
| Tráfico por microservicio crítico | ~500 req/min (≈ 8,3 req/s) | ~5.000 req/min (≈ 83 req/s) — **×10** |
| Latencia síncrona p95 | desconocida (medir, ver §1.4) | **< 500 ms** |
| Latencia asíncrona (publicación → consumo) | desconocida | **< 2 s** |
| Disponibilidad | sin SLO formal | **99,9%** (≤ 43 min caída/mes) |
| Presupuesto | mínimo viable | optimizado, prioriza estabilidad |

**Dimensionamiento por Ley de Little:** concurrencia ≈ tasa × latencia. A 83 req/s con latencia media de 200 ms ⇒ ~17 peticiones concurrentes por servicio crítico. Es una carga **moderada** para Spring Boot con virtual threads; el riesgo real no está en el cómputo sino en los **recursos compartidos**: conexiones a RDS, broker MQ y Keycloak (ver Fase 1).

### 1.4 Relevamiento real de la cuenta AWS (ejecutado 2026-06-11)

Los datos que en la primera versión figuraban como hipótesis se verificaron directamente contra la cuenta `160631388633` (us-east-1, cluster `industrial-safety-cluster`):

| Dato | Valor real verificado | Implicación |
|---|---|---|
| Tareas ECS (todas las apps) | **0,5 vCPU / 1 GB** (512/1024 MiB), Fargate, 13 servicios definidos | Coincide con el tamaño base recomendado en §4.1 ✔ |
| Keycloak | Servicio ECS propio (`industrial-safety-keycloak-task-service`), **0,5 vCPU / 2 GB**, 1 réplica | CPU corta para logins masivos (hashing es CPU-bound); subir a 1 vCPU y 2 réplicas (§4.4) |
| Estado del cluster | **0 tareas corriendo** al relevar (entorno apagado para ahorrar); capacity providers FARGATE y **FARGATE_SPOT** | Spot disponible para servicios no críticos (§4.1) |
| Auto Scaling | **Ningún scalable target registrado** (`describe-scalable-targets` → vacío) | Confirmado: capacidad fija; acción de prioridad ALTA |
| RDS | **UNA sola instancia compartida** `db-industrial-safety`: **db.t4g.micro** (1 GiB → ~112 max_connections), **single-AZ**, 20 GB **gp2**, detenida al relevar; Performance Insights ya habilitado ✔ | El escenario compartido de §2.3 confirmado, con la clase más chica posible — riesgo nº 1 agravado |
| Amazon MQ | `industrial-security-rabbitmq`: RabbitMQ **mq.m7g.medium SINGLE_INSTANCE**, estado **RUNNING** | Factura 24/7 aunque todo lo demás esté apagado (Amazon MQ no se puede "detener"): costo fijo ~US$60–75/mes |
| Cloud Map | Namespace privado `industrial-security`, 11 servicios registrados, **TTL = 10 s** en todos | Correcto para autoscaling ✔ — sin cambios |
| Container Insights | **Ya habilitado** en el cluster ✔ | Acción de Fase 4 parcialmente cubierta; evaluar modo `enhanced` |
| Exposición pública | Servicio **`ngrok-tunnel-service`** en ECS (0,25 vCPU / 512 MB) hacia el dominio DuckDNS | Workaround de desarrollo en la ruta de producción: punto único de fallo, sin SLA (ver H8) |
| EC2 / Aurora / DocumentDB / ElastiCache | **No existen** instancias ni clusters | MongoDB (course, chat) y Redis (course) **no tienen equivalente provisionado en AWS** — confirmar si usan MongoDB Atlas externo o si falta provisionarlos. **Única incógnita restante del plan** |

> Nota operativa: una instancia RDS detenida **se reinicia sola a los 7 días** (límite de AWS) y vuelve a facturar cómputo; el almacenamiento se factura siempre, esté detenida o no.

---

## 2. FASE 1 — Diagnóstico de capacidad actual

### 2.1 Hallazgos reales en el código (corregir antes de escalar)

Estos hallazgos salen de la inspección del repositorio y son **acciones de costo cero o casi cero** con impacto directo en rendimiento:

| # | Hallazgo | Evidencia | Impacto | Acción |
|---|---|---|---|---|
| H1 | **Logging DEBUG/TRACE en el api-gateway** (gateway, security, web reactive) | `api-gateway/src/main/resources/application.yaml:30-36` | Cada petición genera decenas de líneas de log: degrada throughput del gateway (componente por el que pasa TODO el tráfico) e infla el costo de CloudWatch Logs (~$0,50/GB ingerido) | ✔ **Corregido (2026-06-11)**: niveles INFO/WARN; para depurar, override vía Parameter Store sin tocar código |
| H2 | **JVM sin flags de memoria** (`java -jar app.jar`) | `*/Dockerfile` ENTRYPOINT | Con los defaults, el heap es ~25% de la memoria del contenedor: una tarea de 1 GB usa ~256 MB de heap y desperdicia el resto; bajo carga provoca GC frecuente u OOM | ✔ **Corregido (2026-06-11)**: `-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError` en los 11 Dockerfiles desplegados |
| H3 | **HikariCP sin configurar en 6 de los 7 servicios JPA** (solo user-service lo tenía) | `application.yml/.yaml` de safety, order, payment, exam, solicitudes, purchase | Pool por defecto = **10 conexiones siempre abiertas por instancia**. Con autoscaling esto explota contra `max_connections` de RDS (ver §2.3). Además, con virtual threads el pool JDBC pasa a ser el cuello real | ✔ **Corregido**: pool explícito (10/2, exam 5/1) en los 6 servicios |
| H4 | **`ddl-auto: update` en producción** | `safety-service/application.yml:17` (patrón repetido) | Hibernate ejecuta DDL al arrancar: locks en tablas durante despliegues, arranques lentos, esquema no reproducible | Migrar a Flyway + `ddl-auto: validate` |
| H5 | **Doble compilación en CD** (mvn package en el workflow + rebuild dentro del Dockerfile multi-stage) | `deploy.yml:62-64` + `Dockerfile:1-5` | Duplica el tiempo de pipeline ×11 servicios (capacidad de CI desperdiciada, despliegues lentos) | ✔ **Corregido (2026-06-11)**: Dockerfiles single-stage que copian `target/*.jar` del workflow. **Nota**: `docker build` local ahora requiere `./mvnw package -DskipTests` previo |
| H6 | **Keycloak expuesto por HTTP sin TLS** y con issuer en DuckDNS | `api-gateway/application.yaml:22` | Tokens viajan en claro; además `start-dev` (visto en compose) no debe llegar a producción | `start --optimized` + HTTPS (ACM + ALB) — ver Fase 3 §4.4 |
| H7 | Virtual threads habilitados ✔ y colas durables con DLQ ✔ | `application.yml`, `RabbitMQConfig.java` | **Fortalezas**: base correcta para concurrencia I/O-bound y resiliencia de mensajería | Mantener; tunear prefetch/concurrency (Fase 3 §4.3) |
| H8 | **Túnel ngrok como exposición pública** (servicio ECS `ngrok-tunnel-service`) | Relevamiento AWS (§1.4) | Punto único de fallo sin SLA en la ruta de entrada, latencia extra; DuckDNS + ngrok no soportan la campaña ×10 | Reemplazar por ALB público + certificado ACM + Route 53 antes de la campaña (también resuelve el TLS de H6) |
| H9 | **Config fuera del estándar del proyecto**: solicitudes-service lleva hardcodeados el endpoint RDS y el usuario `postgres` con password fallback `admin`; purchase-service importa config de un config-server en `localhost` | `solicitudes-service/application.yaml:13-16`, `purchase-service/application.yaml:5` | Credenciales en el repositorio (riesgo de seguridad) y configuración no gestionable por entorno | Migrar ambos a Parameter Store (`/config/<servicio>/`) como el resto; rotar la contraseña del usuario `postgres` |

### 2.2 Cuellos de botella típicos por componente y métricas CloudWatch críticas

#### a) ECS (Fargate) — microservicios Spring Boot

| Cuello de botella | Por qué ocurre aquí | Métrica CloudWatch crítica |
|---|---|---|
| CPU throttling de la tarea | JVM + Spring arranca con picos de CPU; tareas pequeñas (0,25 vCPU) alargan el arranque a minutos y fallan health checks durante el escalado | `AWS/ECS CPUUtilization` (por servicio); con Container Insights: `CpuUtilized` vs `CpuReserved` por tarea |
| Memoria insuficiente / OOMKill | H2: heap mal dimensionado; metaspace + threads + buffers fuera del heap | `MemoryUtilization`; Container Insights: `MemoryUtilized`, eventos `OutOfMemoryError` en logs |
| Arranque lento → escalado tardío | El autoscaling solo sirve si la tarea nueva está sana en <2 min | `ServiceDesiredCount` vs `RunningTaskCount`; tiempo hasta `HealthyHostCount` en el target group |
| Gateway saturado | Todo el tráfico pasa por api-gateway (y hoy loguea en DEBUG: H1) | `TargetResponseTime` p95/p99 del ALB, `HTTPCode_Target_5XX_Count` |

#### b) RDS PostgreSQL

| Cuello de botella | Por qué ocurre aquí | Métrica crítica |
|---|---|---|
| **Agotamiento de conexiones** | Ver §2.3 — el riesgo nº 1 de esta arquitectura | `DatabaseConnections` vs `max_connections` |
| CPU/credit burst agotado (clases t) | Las clases `db.t3/t4g` funcionan por créditos: una campaña ×10 sostenida los agota y la CPU cae al baseline | `CPUUtilization`, `CPUCreditBalance`, `CPUSurplusCreditBalance` |
| IOPS/throughput de storage | Consultas sin índice (`show-sql: false` oculta el problema) + `ddl-auto: update` | `ReadIOPS`, `WriteIOPS`, `ReadLatency`, `WriteLatency`, `DiskQueueDepth` |
| Memoria / swap | `work_mem` por defecto con consultas de ordenamiento grandes | `FreeableMemory`, `SwapUsage` |

#### c) Amazon MQ (RabbitMQ)

| Cuello de botella | Por qué ocurre aquí | Métrica crítica (namespace `AWS/AmazonMQ`) |
|---|---|---|
| Backlog de colas (consumidores lentos) | notification-service consume 3 colas (email, cert, ws-alert); el envío de email es lento por naturaleza | `MessageReadyCount`, `MessageUnacknowledgedCount` (por cola), `MessageCount` |
| Memoria del broker → flow control | RabbitMQ bloquea publishers al superar la marca de memoria | `RabbitMQMemUsed` vs `RabbitMQMemLimit` |
| Disco del broker | Colas durables persisten a disco; backlog grande = disco lleno = broker bloqueado | `RabbitMQDiskFree` vs `RabbitMQDiskFreeLimit` |
| Conexiones/canales | 8 servicios × N tareas × canales por listener | `ConnectionCount`, `ChannelCount` |
| CPU del broker | Clases `mq.t3` también funcionan con burst | `SystemCpuUtilization` |

#### d) Keycloak

| Cuello de botella | Por qué ocurre aquí | Métrica/fuente |
|---|---|---|
| Emisión de tokens (logins) | El gateway valida JWT **localmente** (firma vía JWKS, cacheada por Spring) — eso escala bien. La carga real de Keycloak es la **emisión**: logins, refresh tokens, password hashing (PBKDF2/argon2 es CPU-intensivo a propósito) | CPU de la tarea ECS de Keycloak; métricas propias con `KC_METRICS_ENABLED=true` (endpoint `/metrics`) |
| Su base de datos | Cada login/refresh toca `keycloak-db` | `DatabaseConnections`, `CPUUtilization` de la instancia RDS de Keycloak |
| Sesiones en memoria (single node) | Con 1 réplica, un reinicio invalida sesiones SSO activas | `RunningTaskCount` del servicio Keycloak |

#### e) Cloud Map

No es un cuello típico (la resolución DNS la sirve Route 53 Resolver dentro de la VPC), pero hay que vigilar:

| Riesgo | Detalle | Acción |
|---|---|---|
| TTL del registro DNS vs escalado | Si el TTL es alto (>60 s), las tareas nuevas tardan en recibir tráfico y las terminadas siguen recibiendo | Verificar TTL ≤ 10–15 s: `aws servicediscovery get-service --id <id>` |
| Cuotas de la API | `DiscoverInstances` tiene cuota de TPS por cuenta (solo aplica si se usa descubrimiento por API en vez de DNS) | Revisar en Service Quotas → AWS Cloud Map; preferir resolución DNS |
| Registros huérfanos | Tareas mal desregistradas tras despliegues forzados | Auditar `aws servicediscovery list-instances --service-id <id>` tras cada deploy |

### 2.3 La matemática de conexiones (el riesgo nº 1)

Pool Hikari por defecto = 10 conexiones/instancia (H3). `max_connections` en RDS PostgreSQL ≈ `LEAST(memoria_bytes/9531392, 5000)`:

| Clase RDS | RAM | max_connections aprox. |
|---|---|---|
| db.t4g.micro | 1 GiB | ~112 |
| db.t4g.small | 2 GiB | ~225 |
| db.t4g.medium | 4 GiB | ~450 |

**Escenario confirmado por el relevamiento (§1.4): instancia única compartida db.t4g.micro (~112 conexiones máx.).** La aritmética es lapidaria: HikariCP por defecto mantiene las 10 conexiones abiertas por instancia (minimum-idle = maximum-pool-size), así que con solo 7 servicios JPA × 1 tarea + Keycloak (~20) ≈ **90 conexiones en reposo — el 80% del límite sin tráfico alguno**. Escalar cualquier servicio a 2 tareas, o el escenario campaña (7 × 3 tareas × 10 + Keycloak = 230), rebasa el límite con holgura. Síntoma esperable: `FATAL: remaining connection slots are reserved` → errores 500 en cascada justo en el pico. Conclusión operativa: **no se puede activar el autoscaling de la Fase 3 sin ejecutar antes las dos mitigaciones** (pools explícitos pequeños + subir la clase de instancia).

**Mitigación (Fase 3):** pools explícitos y pequeños (5–10), dimensionar la instancia, y evaluar RDS Proxy.

---

## 3. FASE 2 — Análisis de rendimiento (plan de pruebas de carga)

### 3.1 Herramienta recomendada

**k6 (Grafana)** como herramienta principal: scripts en JavaScript versionables en el repo, umbrales (`thresholds`) que fallan el pipeline automáticamente, y bajo consumo de recursos. Alternativas válidas: JMeter (si el equipo prefiere GUI) o la solución **AWS Distributed Load Testing** (CloudFormation, genera carga desde Fargate — útil para el escenario ×10 sin saturar la máquina local).

**Regla de oro:** las pruebas se ejecutan contra **staging** (réplica reducida de producción), nunca contra producción, y siempre atravesando el api-gateway con tokens reales de Keycloak (si se omite la autenticación, los resultados no sirven).

### 3.2 Escenarios de prueba específicos de esta arquitectura

| ID | Escenario | Qué simula | Carga objetivo | Criterio de éxito |
|---|---|---|---|---|
| LT-01 | **Login masivo Keycloak** | Campaña: usuarios autenticándose a la vez. Flujo `password` o `client_credentials` contra `/realms/industrial-safety/protocol/openid-connect/token` | Ramp 0→50 logins/s en 5 min, sostener 10 min | p95 emisión token < 1 s; 0 errores 5xx; CPU Keycloak < 75% |
| LT-02 | **API síncrona vía gateway** | CRUD de los servicios críticos (user, safety, solicitudes) con JWT válido, atravesando api-gateway + Cloud Map | Ramp 8→83 req/s por servicio (el ×10), sostener 15 min | **p95 < 500 ms**, error rate < 1% |
| LT-03 | **Inyección a RabbitMQ** | Pico de eventos: crear órdenes/solicitudes que publican a `ORDER_CREATED_QUEUE` y `SOLICITUDES_QUEUE` más rápido de lo que consumen payment/notification | 100 → 500 msg/s durante 10 min | Latencia publicación→ack consumidor **< 2 s**; `MessageReadyCount` vuelve a ~0 en < 5 min tras el pico; DLQs vacías |
| LT-04 | **Consultas pesadas PostgreSQL** | Reportes/listados con filtros y paginación sobre las tablas más grandes (sembrar datos sintéticos: ≥ 1M filas en tablas de eventos/solicitudes) | 20 consultas pesadas/s concurrentes con LT-02 activo | p95 de la consulta < 500 ms; `DatabaseConnections` < 70% del máximo; sin `DiskQueueDepth` sostenido |
| LT-05 | **Resiliencia al escalado (Cloud Map)** | Durante LT-02, forzar un escalado (`aws ecs update-service --desired-count +2`) y matar una tarea (`aws ecs stop-task`) | Carga LT-02 constante | Error rate < 1% durante la transición; sin errores de resolución DNS; tráfico llega a tareas nuevas en < 30 s |

### 3.3 Ejemplo de script k6 (LT-02, con autenticación real)

```javascript
// load/lt02-api-sync.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '5m', target: 50 },   // ramp
    { duration: '15m', target: 50 },  // sostenido (~83 req/s con sleep)
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],   // SLO p95
    http_req_failed: ['rate<0.01'],     // < 1% errores
  },
};

export function setup() {
  const res = http.post(`${__ENV.KEYCLOAK_URL}/realms/industrial-safety/protocol/openid-connect/token`,
    { grant_type: 'password', client_id: __ENV.CLIENT_ID,
      username: __ENV.TEST_USER, password: __ENV.TEST_PASS });
  return { token: res.json('access_token') };
}

export default function (data) {
  const res = http.get(`${__ENV.GATEWAY_URL}/api/v1/solicitudes`,
    { headers: { Authorization: `Bearer ${data.token}` } });
  check(res, { 'status 200': (r) => r.status === 200 });
}
```

### 3.4 Interpretación de resultados y umbrales de alerta

| Síntoma observado | Diagnóstico probable | Dónde confirmar |
|---|---|---|
| p95 sube linealmente con la carga, CPU ECS < 50% | Cuello en pool de conexiones (Hikari) o en RDS | Métrica `hikaricp.connections.pending` (Actuator) y `DatabaseConnections` |
| p95 estable pero p99 con picos periódicos | GC de la JVM (heap pequeño, H2) | `jvm.gc.pause` en Actuator/Micrometer |
| Errores 5xx solo durante escalado | Tareas reciben tráfico antes de estar listas o TTL DNS alto | Health check grace period + TTL Cloud Map (LT-05) |
| `MessageReadyCount` crece y no baja | Consumidores insuficientes (concurrency) o broker en flow control | `RabbitMQMemUsed`, concurrencia del listener |
| Latencia de login > 1 s con CPU Keycloak al 100% | Hashing de passwords saturando CPU | Subir CPU de la tarea o réplicas de Keycloak |
| Todo bien hasta N req/s y luego colapso total | Agotamiento de conexiones RDS (§2.3) | Logs PostgreSQL: `remaining connection slots` |

**Umbrales de alerta derivados** (se formalizan en Fase 4): actuar cuando cualquier recurso supere el **70% sostenido** durante la prueba al nivel de carga objetivo — esa es la señal de que no hay margen para el ×10.

---

## 4. FASE 3 — Planificación de capacidad en AWS (soluciones concretas)

### 4.1 ECS Fargate (microservicios Spring Boot)

**Decisión: Fargate** (no EC2): elimina gestión de instancias, el equipo es pequeño y el patrón de carga es elástico. Costo unitario mayor que EC2, pero el costo operativo humano lo compensa con creces a esta escala.

**Dimensionamiento recomendado por tarea:**

| Servicio | vCPU / Mem actual (verificado §1.4) | Recomendado | Min / Max tareas | Justificación |
|---|---|---|---|---|
| api-gateway | 0,5 vCPU / 1 GB | **1 vCPU / 2 GB** (subir) | 2 / 8 | Pasa todo el tráfico; WebFlux/Netty aprovecha más CPU |
| user, safety, solicitudes, order, payment, purchase (críticos) | 0,5 vCPU / 1 GB | **0,5 vCPU / 1 GB** (mantener ✔) | 2 / 6 | Con virtual threads y 83 req/s I/O-bound sobra; min=2 por disponibilidad 99,9% (multi-AZ) |
| course, exam, chat, notification | 0,5 vCPU / 1 GB | **0,5 vCPU / 1 GB** (mantener ✔) | 1 / 4 | Menor criticidad; notification escala por backlog MQ, no por CPU |
| Keycloak | 0,5 vCPU / 2 GB, 1 réplica | **1 vCPU / 2 GB** (subir CPU) | 2 / 4 | Hashing de credenciales es CPU-bound; 2 réplicas para HA |

> Nota 1: min=1 en los no críticos sacrifica redundancia por costo. Si el presupuesto lo permite, min=2 en todo lo que esté en la ruta crítica de la campaña.
> Nota 2: el cluster ya tiene el capacity provider **FARGATE_SPOT** habilitado (verificado §1.4): usarlo para los servicios no críticos (course, exam, chat) con una estrategia mixta (p. ej. base 1 tarea FARGATE + el resto SPOT) reduce ~70% el costo de esas tareas, aceptando posibles interrupciones con 2 min de aviso.

**Cómo hacerlo — Auto Scaling con Application Auto Scaling + CloudWatch (CLI):**

```bash
# 1. Registrar el servicio como objetivo escalable (ejemplo: safety-service)
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/$ECS_CLUSTER/safety-service-service-6r85geni \
  --min-capacity 2 --max-capacity 6

# 2. Política target-tracking por CPU (mantener 60% promedio)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/$ECS_CLUSTER/safety-service-service-6r85geni \
  --policy-name cpu-target-60 \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 60.0,
    "PredefinedMetricSpecification": {"PredefinedMetricType": "ECSServiceAverageCPUUtilization"},
    "ScaleOutCooldown": 60,
    "ScaleInCooldown": 300
  }'
```

Para el **api-gateway**, añadir una segunda política por peticiones (si está detrás de ALB): `PredefinedMetricType: ALBRequestCountPerTarget` con `TargetValue` ≈ 1.500 req/target/min. Para **notification-service**, escalar por backlog: alarma sobre `MessageReadyCount` de `EMAIL_QUEUE` → step scaling (no hay target-tracking nativo para Amazon MQ).

**Estrategia de despliegue — mantener rolling update, endurecido:**

Blue/green (CodeDeploy) añade complejidad que este equipo no necesita aún. El rolling actual (`force-new-deployment`) es correcto; falta el **circuit breaker de despliegue** para rollback automático:

```bash
aws ecs update-service \
  --cluster $ECS_CLUSTER \
  --service safety-service-service-6r85geni \
  --deployment-configuration \
  'deploymentCircuitBreaker={enable=true,rollback=true},maximumPercent=200,minimumHealthyPercent=100'
```

Con `minimumHealthyPercent=100` y `maximumPercent=200` el despliegue nunca reduce capacidad (clave durante la campaña). Configurar también `healthCheckGracePeriodSeconds=120` (Spring Boot tarda en arrancar) y health check del target group apuntando a `/actuator/health`.

**Cambios en Dockerfile (H2, H5):**

```dockerfile
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar          # el JAR ya lo compiló el workflow (elimina doble build)
EXPOSE 8099
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
```

### 4.2 RDS PostgreSQL

**Provisionado vs Serverless v2 — decisión razonada:**

| Criterio | Provisionado (db.t4g/m7g) | Aurora Serverless v2 |
|---|---|---|
| Patrón de carga | Constante o predecible ✔ (campaña conocida con 3 meses de aviso) | Picos impredecibles |
| Costo a esta escala | ~$24–95/mes por instancia | Mínimo 0.5 ACU ≈ $43/mes y sube rápido; Aurora cobra I/O aparte |
| Complejidad | Baja ✔ | Media (migración a Aurora) |
| **Veredicto** | **Provisionado.** La campaña es planificable: se escala la clase de instancia 1–2 semanas antes (`modify-db-instance`, con Multi-AZ el cambio es ~sin downtime) | Reevaluar solo si el tráfico se vuelve impredecible |

**Dimensionamiento (confirmado en §1.4: instancia única compartida `db-industrial-safety`, db.t4g.micro, single-AZ, 20 GB gp2):**

- La consolidación en una instancia ya existe (correcto por costo), pero la clase es insuficiente incluso para el estado actual (§2.3). Ruta recomendada: subir ya a **db.t4g.medium Multi-AZ** (4 GiB, ~450 conexiones) y, dos semanas antes de la campaña, a **db.m7g.large** (8 GiB, sin créditos burst, ~900 conexiones), bajando al terminar. Con Multi-AZ el cambio de clase es ~sin downtime: `aws rds modify-db-instance --db-instance-identifier db-industrial-safety --db-instance-class db.t4g.medium --multi-az --apply-immediately`.
- **Migrar storage gp2 → gp3 en el mismo cambio:** a 20 GB, gp2 entrega solo 100 IOPS de base; gp3 da 3.000 IOPS por igual o menor precio. Es la mejora de I/O más barata disponible: `--storage-type gp3`.
- Recordatorio operativo (§1.4): la instancia detenida se reinicia sola a los 7 días; el storage se factura siempre.

**Réplicas de lectura:** solo si LT-04 muestra que las consultas de lectura (reportes, listados) dominan y saturan la primaria. Implementación: réplica + `AbstractRoutingDataSource` en los servicios de reporting. **No** crearlas preventivamente (costo doble sin evidencia).

**Pool de conexiones (corrige H3)** — añadir en cada servicio (o en `/config/application/` de Parameter Store para todos):

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # explícito; gateway y servicios de poco tráfico: 5
      minimum-idle: 2
      connection-timeout: 5000   # fallar rápido, no encolar indefinidamente
      max-lifetime: 900000       # 15 min, menor que cualquier timeout intermedio
```

Presupuesto de conexiones resultante (peor caso campaña): 6 servicios críticos × 6 tareas × 10 + 5 servicios × 4 × 5 + Keycloak 20 ≈ **480** → confirma que la consolidada necesita **db.m7g.large (~900 conexiones)** durante la campaña, o **RDS Proxy** (multiplexa conexiones; ~$22/mes sobre una instancia de 2 vCPU) si se quiere mantener instancia chica.

**Parámetros (parameter group personalizado):**

```bash
aws rds create-db-parameter-group --db-parameter-group-name safeindustrial-pg16 \
  --db-parameter-group-family postgres16 --description "Tuning SafeIndustrial"
aws rds modify-db-parameter-group --db-parameter-group-name safeindustrial-pg16 \
  --parameters \
  "ParameterName=work_mem,ParameterValue=16384,ApplyMethod=immediate" \
  "ParameterName=log_min_duration_statement,ParameterValue=500,ApplyMethod=immediate"
```

(`log_min_duration_statement=500` registra toda consulta > 500 ms = el SLO; es el detector de consultas lentas más barato que existe.)

**Performance Insights:** ya está habilitado en `db-industrial-safety` (verificado §1.4) ✔ — usarlo activamente durante las pruebas LT-04 para identificar las consultas top por carga. Comando de referencia si se recrea la instancia:

```bash
aws rds modify-db-instance --db-instance-identifier db-industrial-safety \
  --enable-performance-insights --performance-insights-retention-period 7 \
  --apply-immediately
```

### 4.3 Amazon MQ (RabbitMQ)

**Dimensionamiento del broker (actual verificado §1.4: `mq.m7g.medium` SINGLE_INSTANCE, RUNNING):**

El volumen proyectado (~centenas de msg/s en pico, §LT-03) es bajo para RabbitMQ: **el m7g.medium actual es suficiente en throughput** incluso para la campaña (validar con LT-03). La decisión pendiente la dicta la **disponibilidad**, no el rendimiento:

| Opción | Costo aprox./mes | Cumple 99,9% | Cuándo |
|---|---|---|---|
| **`mq.m7g.medium` single-instance (actual)** | ~$60–75 | ✘ (mantenimientos = minutos de downtime) | Mantener, con la excepción al SLO documentada |
| `mq.t3.micro` single-instance | ~$27 | ✘ (AWS lo limita a dev/test) | Solo staging |
| Clúster 3 nodos (`m7g.medium`/`m5.large`) | ~$200–630 | ✔ | Solo si el 99,9% es contractual |

**Recomendación pragmática:** mantener el broker actual **y documentar la excepción al SLO** (ventanas de mantenimiento de Amazon MQ = minutos de indisponibilidad de mensajería; las colas durables + DLQ ya existentes evitan pérdida de mensajes, los publishers deben tener retry con backoff — verificar que los `RabbitTemplate` lo tengan).

> Costo fijo a vigilar: el broker **factura 24/7 aunque el resto del entorno esté apagado** (Amazon MQ no se puede detener, y hoy está RUNNING con 0 tareas ECS activas). Si el entorno pasa semanas apagado entre sesiones del curso, evaluar recrearlo por script/IaC al inicio de cada ciclo de trabajo — o aceptar los ~$60–75/mes como costo base.

**Configuración de colas — lo que ya está bien y lo que falta:**

- ✔ Colas **durables** con **DLQ** ya configuradas en todos los servicios (`QueueBuilder.durable(...)`). Mantener.
- ✔ Verificado en código: order, payment, exam y notification ya traen `acknowledge-mode: manual`, `prefetch: 10`, retry con backoff y publisher confirms. Lo que faltaba era **concurrencia** en notification-service (el consumidor pesado: 3 colas y envío de email lento) — ✔ **corregido (2026-06-11)**: `concurrency: 2` / `max-concurrency: 8`. Referencia completa para los demás listeners:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10                # límite de mensajes sin ack por consumidor
        concurrency: 2              # consumidores mínimos por listener
        max-concurrency: 8          # crece bajo backlog
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
    publisher-confirm-type: correlated   # confirmación de publicación
    publisher-returns: true
```

- **Colas efímeras vs duraderas:** mantener duraderas para todo lo de negocio (órdenes, pagos, solicitudes, certificados). Candidata a **no duradera/TTL corto**: `WS_ALERT_QUEUE` si las alertas WebSocket pierden valor en segundos — añadirle `x-message-ttl: 30000` para que el backlog viejo se descarte solo.

**Monitoreo (las métricas ya llegan a CloudWatch automáticamente):** alarmas sobre `MessageReadyCount` y `MessageUnacknowledgedCount` por cola — ver Fase 4.

### 4.4 Keycloak

1. **Modo producción** (hoy compose usa `start-dev`): imagen construida con `kc.sh build` + `start --optimized`, `KC_DB=postgres` apuntando a su RDS, `KC_HOSTNAME` con el dominio definitivo, TLS terminado en ALB con certificado ACM (resuelve H6). Cambiar el `issuer-uri` del gateway a `https://`.
2. **Escalamiento horizontal:** 2 tareas ECS detrás del ALB. Para que los caches de sesión/realm se sincronicen entre réplicas, Keycloak usa Infinispan embebido; en ECS el descubrimiento de peers se hace con **JDBC_PING** (los nodos se encuentran a través de la propia BD — sin multicast, que Fargate no soporta). Configuración: `--cache=ispn --cache-stack=jdbc-ping` (KC 24 lo soporta vía SPI/XML de Infinispan).
3. **Offload de validación:** ya está bien hecho — el gateway valida JWT con la clave pública (JWKS, que Spring Security cachea); Keycloak **no** recibe una llamada por petición. Mantener **access tokens cortos (5 min) + refresh tokens**, así el costo por petición es cero y el costo de refresh es bajo.
4. **Caché y tuning fino:** subir `users` cache si hay muchos usuarios activos; revisar el hashing (Keycloak 24 default 210k iteraciones PBKDF2 — es el costo de CPU dominante en logins masivos; no bajarlo por seguridad, dimensionar CPU en su lugar).
5. **Métricas:** `KC_METRICS_ENABLED=true` y `KC_HEALTH_ENABLED=true` → health check del target group a `/health/ready`.

### 4.5 Cloud Map

- ✔ **TTL verificado (§1.4): 10 s en los 11 servicios registrados** — correcto para que el autoscaling de la Fase 3 sea efectivo. Sin cambios.
- Cuotas por defecto holgadas para 11 servicios (consultar Service Quotas → AWS Cloud Map si se duplica el número de servicios).
- Añadir al runbook post-despliegue: `aws servicediscovery list-instances --service-id <id>` para detectar registros huérfanos.
- No requiere más inversión: **no es cuello de botella** en esta arquitectura mientras el descubrimiento sea por DNS.

---

## 5. FASE 4 — Plan de monitoreo mejorado (más allá de CloudWatch por defecto)

### 5.1 Habilitaciones (con CLI)

**a) Container Insights para ECS** — **ya habilitado** en `industrial-safety-cluster` (verificado §1.4) ✔. Acción restante: evaluar el modo `enhanced` (métricas por tarea con más granularidad y observabilidad de ciclo de vida):

```bash
aws ecs update-cluster-settings --cluster industrial-safety-cluster \
  --settings name=containerInsights,value=enhanced
```

**b) RDS Performance Insights:** ya cubierto en §4.2 (gratis, retención 7 días).

**c) X-Ray — trazabilidad distribuida entre microservicios:**

Camino recomendado para Spring Boot 4: **Micrometer Tracing + OTLP → ADOT Collector (sidecar) → X-Ray**.

1. Dependencias en cada servicio:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 0.1          # 10% en producción; 1.0 en staging
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces   # sidecar ADOT
```

2. Sidecar ADOT en cada task definition:

```json
{
  "name": "aws-otel-collector",
  "image": "public.ecr.aws/aws-observability/aws-otel-collector:latest",
  "command": ["--config=/etc/ecs/ecs-xray.yaml"],
  "essential": true
}
```

3. Permiso `AWSXRayDaemonWriteAccess` en el task role. Resultado: mapa de servicios completo gateway → servicio → RDS/MQ, con la latencia de cada salto — exactamente lo que falta hoy para diagnosticar p95.

> Implementarlo primero en la cadena crítica (api-gateway → solicitudes-service → notification) y extender después; no hace falta el big bang en 11 servicios.

**d) Dashboard CloudWatch personalizado** — uno por capa: "SafeIndustrial — API" (latencia ALB p95/p99, 5xx por servicio, RunningTaskCount), "SafeIndustrial — Datos" (RDS connections/CPU/IOPS, MQ backlog), "SafeIndustrial — Auth" (CPU Keycloak, latencia /token). Crear con `aws cloudwatch put-dashboard --dashboard-name SafeIndustrial-API --dashboard-body file://dashboard-api.json`.

### 5.2 KPIs del servicio

| KPI | Fuente | Objetivo | Frecuencia de revisión |
|---|---|---|---|
| Tasa de errores 5xx por microservicio | ALB `HTTPCode_Target_5XX_Count` / X-Ray | < 0,5% | Diaria (dashboard) / alarma |
| Latencia p95 / p99 APIs | ALB `TargetResponseTime` + X-Ray | p95 < 500 ms; p99 < 1 s | Diaria / alarma |
| Backlog RabbitMQ (`MessageReadyCount` + `MessageUnacknowledgedCount`) | CloudWatch `AWS/AmazonMQ` | < 100 sostenido; drenaje < 5 min tras pico | Alarma |
| Mensajes en DLQ | `MessageCount` de cada `*_DLQ` | 0 (cada mensaje en DLQ es un incidente a revisar) | Alarma |
| Tiempo de emisión de token Keycloak | k6 sintético periódico (LT-01 reducido) o métricas KC | p95 < 1 s | Semanal |
| Conexiones RDS | `DatabaseConnections` | < 70% de max_connections | Alarma |
| Disponibilidad mensual | Health checks Route 53 / ALB | ≥ 99,9% | Mensual (informe) |
| Cobertura JaCoCo | Pipeline CI | ≥ 80% (gate existente — mantener) | Por commit |
| Saturación ECS (CPU/mem por servicio) | Container Insights | < 70% sostenido | Alarma |

### 5.3 Alertas proactivas (SNS)

```bash
# Topic + suscripción
aws sns create-topic --name safeindustrial-alarms
aws sns subscribe --topic-arn arn:aws:sns:us-east-1:<account>:safeindustrial-alarms \
  --protocol email --notification-endpoint equipo@safeindustrial.pe

# CPU ECS > 70% por 5 min (repetir por servicio crítico)
aws cloudwatch put-metric-alarm --alarm-name ecs-safety-cpu-high \
  --namespace AWS/ECS --metric-name CPUUtilization \
  --dimensions Name=ClusterName,Value=$ECS_CLUSTER Name=ServiceName,Value=safety-service-service-6r85geni \
  --statistic Average --period 300 --evaluation-periods 1 \
  --threshold 70 --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:<account>:safeindustrial-alarms

# Conexiones RDS cerca del límite (ejemplo: 315 = 70% de db.t4g.medium)
aws cloudwatch put-metric-alarm --alarm-name rds-connections-high \
  --namespace AWS/RDS --metric-name DatabaseConnections \
  --dimensions Name=DBInstanceIdentifier,Value=<id> \
  --statistic Maximum --period 300 --evaluation-periods 2 \
  --threshold 315 --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:<account>:safeindustrial-alarms

# Backlog de mensajes > 1000 (nombres reales de colas en scripts/apply-capacity-plan.ps1)
aws cloudwatch put-metric-alarm --alarm-name mq-email-backlog \
  --namespace AWS/AmazonMQ --metric-name MessageReadyCount \
  --dimensions Name=Broker,Value=industrial-security-rabbitmq Name=VirtualHost,Value=/ Name=Queue,Value=notification.email.queue \
  --statistic Maximum --period 300 --evaluation-periods 1 \
  --threshold 1000 --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:<account>:safeindustrial-alarms
```

Alarmas adicionales recomendadas: `5XX > 1%` en ALB, `FreeableMemory RDS < 256 MB`, `RabbitMQDiskFree` bajo, `RunningTaskCount < DesiredCount` por 5 min, y **una alarma por cada DLQ con umbral ≥ 1**. Integrar el topic con OpsCenter (Systems Manager) si se quiere gestión formal de incidentes: `aws ssm create-ops-item` desde una regla de EventBridge sobre la alarma.

---

## 6. FASE 5 — Pruebas de rendimiento en el pipeline

**Principio:** el gate de JaCoCo 80% protege la *calidad funcional*; falta el gate de *calidad no funcional*. Se añade una etapa de carga en staging **post-deploy**, con umbrales que rompen el pipeline.

Nueva etapa en GitHub Actions (tras el deploy a staging, antes de promover a producción — o como workflow programado nocturno para no alargar cada deploy):

```yaml
  performance-test:
    name: "Performance gate — staging"
    needs: deploy-staging
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run k6 smoke + load
        uses: grafana/k6-action@v0.3.1
        with:
          filename: load/lt02-api-sync.js
        env:
          GATEWAY_URL: ${{ secrets.STAGING_GATEWAY_URL }}
          KEYCLOAK_URL: ${{ secrets.STAGING_KEYCLOAK_URL }}
          CLIENT_ID: ${{ secrets.LOAD_CLIENT_ID }}
          TEST_USER: ${{ secrets.LOAD_TEST_USER }}
          TEST_PASS: ${{ secrets.LOAD_TEST_PASS }}
      # k6 falla con exit code != 0 si p(95)>=500ms o error rate >= 1%
      # → el job falla → no se promueve a producción
```

**Cadencia recomendada:**

| Prueba | Cuándo | Duración |
|---|---|---|
| Smoke de rendimiento (1 min, 10 VUs, mismos thresholds) | Cada deploy a staging | ~2 min |
| Carga completa LT-01…LT-05 | Semanal (workflow `schedule`) + antes de cada hito de campaña | ~1 h |
| Estrés (hasta el punto de ruptura) | 1 mes antes de la campaña, y tras cambios de dimensionamiento | ~2 h |

Mantener: gate JaCoCo 80% intacto, ejecución en GitHub Actions (ya hay OIDC hacia AWS — no se necesita CodeBuild; sería una herramienta más sin beneficio diferencial).

---

## 7. FASE 6 — Resumen ejecutivo (para stakeholders)

### Diagnóstico en una frase

La arquitectura es sólida en diseño (microservicios desacoplados, mensajería durable con DLQ, JWT validado localmente, CI con gate de cobertura; Container Insights, Performance Insights y Cloud Map con TTL correcto ya activos), pero **el relevamiento sobre la cuenta AWS (§1.4) confirmó que hoy no absorbe un ×10 de tráfico**: no existe ningún autoscaling registrado, la única instancia RDS es una db.t4g.micro compartida single-AZ cuyo límite (~112 conexiones) ya está al ~80% en reposo con los pools por defecto, Keycloak corre en un solo nodo de 0,5 vCPU, la entrada pública depende de un túnel ngrok sin SLA, y hay configuraciones de desarrollo (logging DEBUG, DDL automático, HTTP sin TLS) en la ruta a producción.

### Riesgos principales

| Riesgo | Probabilidad | Impacto |
|---|---|---|
| Agotamiento de conexiones RDS (§2.3) — **confirmado**: db.t4g.micro compartida ya al ~80% del límite en reposo | **Confirmada** | Caída en cascada de servicios críticos |
| Sin autoscaling — **confirmado**: ningún scalable target registrado | **Confirmada** | Latencias > SLO, errores 5xx ante el ×10 |
| Dos puntos únicos de fallo en la ruta crítica: RDS single-AZ y túnel ngrok como entrada pública | Alta | Incompatible con el SLO de 99,9% |
| Keycloak nodo único (0,5 vCPU) en modo dev | Media | Login caído o degradado = plataforma inutilizable |
| Backlog de notificaciones sin alarma | Media | Emails/certificados con horas de retraso, sin que nadie lo sepa |
| Broker MQ single-instance (mq.m7g.medium) | Media | Minutos de indisponibilidad de mensajería en mantenimientos |

### Plan de acción priorizado

| Prioridad | Acción | Esfuerzo | Costo AWS adicional aprox. |
|---|---|---|---|
| **ALTA** (semana 1–2) | ✔ **Hecho (2026-06-11)**: H1, H2, H3 y H5 corregidos en el repo. Pendiente de este grupo: H4 (Flyway) y H9 (config a Parameter Store) | 1–2 días dev restantes | $0 (H1 *ahorra* en CloudWatch Logs) |
| **ALTA** (semana 1–2) | RDS: subir db.t4g.micro → **db.t4g.medium Multi-AZ** + migrar gp2→gp3 (riesgo confirmado §2.3) | 1 día | +$70–85/mes |
| **ALTA** (semana 1–2) | Autoscaling ECS + circuit breaker de despliegue + health checks | 2 días DevOps | Solo el costo de tareas extra bajo pico |
| **ALTA** (semana 2–3) | Alarmas SNS (CPU, conexiones, backlog, DLQ) + dashboards | 1–2 días | ~$5–10/mes |
| **ALTA** (semana 3–4) | Reemplazar túnel ngrok por **ALB público + ACM + Route 53** (H8, y resuelve TLS de H6) | 2–3 días | ~+$20–25/mes (ALB) |
| **ALTA** (semana 3–4) | Keycloak a modo producción + 1 vCPU + 2 réplicas | 3–4 días | ~+$45/mes |
| **MEDIA** (mes 1–2) | X-Ray en cadena crítica + dashboards (Container Insights y Performance Insights **ya están habilitados** ✔) | 1 semana | ~$10–25/mes |
| **MEDIA** (mes 2) | Pruebas de carga LT-01…LT-05 en staging + gate en pipeline | 1 semana | ~$30/mes staging efímero |
| **MEDIA** (pre-campaña) | Escalón final RDS: **db.m7g.large** temporal o RDS Proxy, según LT-04 | 1 día | +$60–90/mes solo durante campaña |
| **BAJA** (pre-campaña) | Clúster Amazon MQ 3 nodos (solo si SLO 99,9% es contractual; el m7g.medium actual alcanza en throughput) | 2 días | +$140–550/mes según clase |
| **BAJA** | Réplicas de lectura RDS (solo si LT-04 lo evidencia) | 3 días | +$47/mes c/u |

### Impacto estimado

- **Rendimiento:** con autoscaling + pools dimensionados + correcciones H1–H4, la plataforma absorbe el ×10 manteniendo p95 < 500 ms (validado por LT-02 antes de la campaña, no por fe).
- **Fiabilidad:** min 2 tareas multi-AZ en críticos + circuit breaker + Keycloak HA acercan el 99,9% real; el broker single-instance queda como excepción documentada.
- **Costo:** la base actual encendida 24/7 ronda **~$300–350/mes** (13 tareas Fargate ≈ $250 + MQ m7g.medium ≈ $65 + RDS micro ≈ $14) — hoy se mitiga apagando el entorno, aunque el broker MQ sigue facturando apagado todo lo demás. El plan añade **~$150–200/mes permanentes** (RDS t4g.medium Multi-AZ + gp3, ALB, Keycloak HA, observabilidad) y **~$150–300/mes temporales** durante el pico de campaña (tareas extra por autoscaling y m7g.large — que es exactamente la gracia: se paga solo mientras dura). FARGATE_SPOT en los servicios no críticos puede recuperar ~$30–50/mes. El clúster MQ es la única decisión cara y queda condicionada al SLO.
- **Detección proactiva:** de "nos enteramos por los usuarios" a alarmas con 15–30 min de anticipación (saturación al 70%) y trazas X-Ray para diagnóstico en minutos.

---

## 8. Anexo — Runbook de relevamiento (✔ ejecutado 2026-06-11, resultados en §1.4 — re-ejecutar tras cada cambio de capacidad)

```bash
# 1. Estado actual de todos los servicios ECS
for s in $(aws ecs list-services --cluster $ECS_CLUSTER --query 'serviceArns[]' --output text); do
  aws ecs describe-services --cluster $ECS_CLUSTER --services $s \
    --query 'services[0].{name:serviceName,desired:desiredCount,running:runningCount,taskDef:taskDefinition}'
done

# 2. CPU/memoria definida por task definition
aws ecs list-task-definitions --query 'taskDefinitionArns[]'
aws ecs describe-task-definition --task-definition <arn> --query 'taskDefinition.{cpu:cpu,memory:memory}'

# 3. Inventario RDS y Amazon MQ
aws rds describe-db-instances --query 'DBInstances[].{id:DBInstanceIdentifier,class:DBInstanceClass,multiAZ:MultiAZ,maxConn:"ver §2.3"}'
aws mq list-brokers

# 4. Línea base de 2 semanas de métricas (antes de tocar nada)
aws cloudwatch get-metric-statistics --namespace AWS/ECS --metric-name CPUUtilization \
  --dimensions Name=ClusterName,Value=$ECS_CLUSTER Name=ServiceName,Value=api-gateway-service-41zhurrw \
  --start-time $(date -u -d '14 days ago' +%Y-%m-%dT%H:%M:%S) --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 --statistics Average Maximum

# 5. Cloud Map: TTL y registros
aws servicediscovery list-services
aws servicediscovery get-service --id <srv-id> --query 'Service.DnsConfig'
```

**Referencias Well-Architected aplicadas:** PERF 1–5 (selección y dimensionamiento de cómputo/datos/red, monitoreo, trade-offs), REL 1–2 y 6–10 (cuotas de servicio, topología de red, monitoreo de carga, diseño para mitigar fallos, pruebas de fiabilidad, DR), COST 5–6 (dimensionar al uso real, escalar con la demanda).
