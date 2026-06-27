# Plan de Gestión de Incidentes — SafeIndustrial

**Proyecto:** Plataforma de Gestión de Seguridad Industrial (SafeIndustrial)
**Marco:** ITIL 4 — Gestión de Incidentes (Incident Management) + ISO/IEC 20000
**Alcance:** proceso de identificación, clasificación, priorización, atención, escalamiento, resolución y cierre de **incidentes de TI / operación** de la plataforma, aterrizado al sistema real.
**Objetivo:** restaurar la operación normal del servicio en el menor tiempo posible, minimizando el impacto al negocio.

---

## 0. Aclaración de alcance — dos sentidos de "incidente"

SafeIndustrial maneja la palabra "incidente" en dos planos. Es importante no confundirlos:

| Tipo | Qué es | De qué trata este plan |
|---|---|---|
| Incidente de **seguridad industrial** (negocio) | Evento de riesgo de un trabajador/planta que gestiona `safety-service` | NO es el foco aquí |
| Incidente de **TI / operación** (plataforma) | Interrupción no planificada del servicio TI: portal caído, login fallando, BD saturada | **✅ Este plan** |

> **Argumento rector:** una plataforma cuyo propósito es **gestionar la seguridad** debe ser ella misma **operativamente confiable**. Si SafeIndustrial se cae, la empresa cliente se queda sin gestión de seguridad. Por eso la gestión de incidentes de TI es crítica para el negocio.

---

## 1. Introducción y contexto (Caso de estudio)

SafeIndustrial es un backend de **microservicios Spring Boot (Java 25)** desplegado en **AWS ECS Fargate**, con base de datos **RDS PostgreSQL**, bus de eventos **Amazon MQ (RabbitMQ)**, autenticación **Keycloak** y descubrimiento de servicios por **Cloud Map**. Esta arquitectura distribuida tiene múltiples puntos de falla, y **ya implementa** mecanismos reales de detección y respuesta a incidentes. Este plan los documenta bajo ITIL e identifica las mejoras pendientes.

### 1.1 Componentes reales que intervienen en la gestión de incidentes

| Componente | Rol en la gestión de incidentes | Evidencia |
|---|---|---|
| **Amazon CloudWatch (Alarmas)** | **Detección 24/7** externa: vigila CPU, RAM, estado de la BD y backlog de colas. Dispara el incidente automáticamente | 17 alarmas activas (ECS CPU>70%, RAM>75%; RDS CPU/disco/memoria) |
| **Amazon SNS** | **Notificación / escalamiento**: avisa al equipo por correo cuando una alarma entra en estado ALARM | Topic `safeindustrial-alarmas` → email del líder técnico |
| **Container Insights (ECS)** | Publica las métricas de CPU/RAM de cada microservicio que CloudWatch evalúa | Habilitado en `industrial-safety-cluster` |
| **RabbitMQ + DLQ** | Resiliencia: un mensaje que falla repetidamente cae a una **Dead Letter Queue** (incidente de integración, no se pierde) | `RabbitMQConfig` de cada servicio; colas `*.dlq` |
| **Deployment Circuit Breaker (ECS)** | **Resolución automática**: si un despliegue nuevo falla el health check, ECS hace **rollback** solo a la versión sana | `apply-capacity-plan.ps1 -CircuitBreaker` |
| **api-gateway + health checks** | Punto único de entrada; los health checks detectan servicios no disponibles | `api-gateway`, rutas RBAC |
| **notification-service** | Comunicación a usuarios/afectados (email, alerta WebSocket) durante un incidente | colas `notification.email`, `notification.ws.alert` |
| **GitHub Actions (CI/CD)** | Trazabilidad de cambios y despliegue con imagen versionada por `:sha` (permite saber qué cambio causó un incidente) | `.github/workflows/deploy.yml` |

### 1.2 Arquitectura del flujo de gestión de incidentes (vista lógica)

```
  ┌──────────────────────────────────────────────────┐
  │  SISTEMA OBSERVADO (interno)                       │   ← solo EMITE métricas
  │  ECS Fargate: api-gateway, user, safety, order,    │
  │  payment, purchase, solicitudes, course, exam,     │
  │  chat, notification, Keycloak                      │
  │  RDS PostgreSQL · Amazon MQ (RabbitMQ)             │
  └───────────────────────┬──────────────────────────┘
                          │  métricas (push automático)
                          ▼
  ┌──────────────────────────────────────────────────┐
  │  CAPA DE GESTIÓN (externa, AWS-managed)            │   ← sigue viva aunque
  │  CloudWatch (DETECTA) → Alarma (CLASIFICA umbral)  │     el cluster caiga
  │                              │                      │
  │                              ▼                      │
  │  SNS topic safeindustrial-alarmas  (NOTIFICA)      │
  │                              │                      │
  │                              ▼                      │
  │   📧 correo al equipo  →  REGISTRO / ticket        │
  └──────────────────────────────────────────────────┘
```

> **Principio de independencia:** la capa de detección (CloudWatch + SNS) es **administrada por AWS y externa** al cluster. Si todo el backend se cae, CloudWatch lo sigue viendo desde afuera y SNS sigue avisando. *No se le puede pedir al sistema caído que avise de su propia caída.*

---

## 2. Identificación y Clasificación de Incidentes

### 2.1 Identificación (¿cómo se detecta?)

| Mecanismo | Qué detecta | En SafeIndustrial |
|---|---|---|
| **Monitoreo (automático)** | CPU/RAM altas, BD saturada, disco lleno | Alarmas CloudWatch (las 17 activas) |
| **Mensajería** | Mensajes que fallan y caen a DLQ | Colas `*.dlq` de RabbitMQ |
| **Health checks** | Servicio no responde / no disponible | api-gateway + ECS health checks |
| **Usuario / Mesa de ayuda** | Reporte manual (no puedo entrar, va lento) | Canal de soporte → ticket |
| **Despliegue fallido** | Una versión nueva rompe el servicio | Circuit breaker ECS (auto-rollback) |

### 2.2 Clasificación (categorías - tipo)

Se reutiliza la taxonomía del PDF, aterrizada a la arquitectura real:

| Categoría | Componente de SafeIndustrial |
|---|---|
| Infraestructura | ECS Fargate (cluster, tareas), red, gateway |
| Aplicaciones | Microservicios Spring Boot, Keycloak (login) |
| Base de Datos | RDS PostgreSQL `db-industrial-safety` |
| Redes y Mensajería | Cloud Map (DNS interno), Amazon MQ (RabbitMQ) |
| Seguridad | Keycloak / autenticación / RBAC del gateway |

### 2.3 Matriz de priorización (Impacto × Urgencia → Prioridad)

- **Impacto:** ¿cuántos usuarios/servicios afecta?
- **Urgencia:** ¿qué tan rápido debe resolverse?

| Impacto \ Urgencia | Alta | Media | Baja |
|---|---|---|---|
| **Alto** | 🔴 Crítica | 🟠 Alta | 🟡 Media |
| **Medio** | 🟠 Alta | 🟡 Media | 🟢 Baja |
| **Bajo** | 🟡 Media | 🟢 Baja | 🟢 Baja |

### 2.4 Identificación y clasificación de los incidentes del caso (SafeIndustrial)

| # | Incidente real | Categoría - Tipo | Detección | Impacto | Urgencia | Prioridad |
|---|---|---|---|---|---|---|
| 1 | **api-gateway no responde** (portal caído) | Infraestructura | Alarma CPU/RAM gateway + health check | Alto | Alta | 🔴 Crítica |
| 2 | **Usuarios no pueden iniciar sesión** (Keycloak/user-service) | Seguridad / Aplicaciones | Health check + reporte usuario | Alto | Media | 🟠 Alta |
| 3 | **Reportes muestran información incorrecta** (safety/order) | Base de Datos / Aplicaciones | Reporte usuario + revisión datos | Medio | Media | 🟡 Media |
| 4 | **Lentitud general del sistema** (CPU/RAM alta, backlog de colas) | Redes y Mensajería / Infraestructura | Alarma RAM>75% + backlog MQ | Bajo | Baja | 🟢 Baja |
| 5 | **RDS con CPU al 98%** (BD saturada) | Base de Datos | Alarma `rds-cpu-db-industrial-safety` | Alto | Alta | 🔴 Crítica |
| 6 | **Mensajes en DLQ** (eventos que fallan: solicitud/pago/notificación) | Redes y Mensajería | Alarma backlog `*.dlq` | Medio | Media | 🟡 Media |

---

## 3. Diseño del proceso y flujo de atención — BPMN

### 3.1 Flujo general (notación BPMN — vista de texto)

```
 (Inicio)
    │
    ▼
┌────────────────┐   evento / métrica
│ Reporte de     │◄── usuario, monitoreo, DLQ, health check
│ incidente      │
└──────┬─────────┘
       ▼
┌────────────────┐
│ Identificación │  CloudWatch / health check / DLQ confirman el evento
└──────┬─────────┘
       ▼
┌────────────────┐
│ Registro       │  se genera ticket: fecha/hora, servicio, métrica, evidencia
└──────┬─────────┘
       ▼
┌────────────────┐
│ Clasificación  │  categoría (Infra/App/BD/Red/Seguridad)
└──────┬─────────┘
       ▼
┌────────────────┐
│ Priorización   │  matriz Impacto×Urgencia → Crítica/Alta/Media/Baja
└──────┬─────────┘
       ▼
   ◇ ¿Prioridad Crítica/Alta? ◇──Sí──► Escalamiento (notifica líder + SNS)
       │ No                              │
       ▼                                 ▼
┌────────────────┐               ┌────────────────┐
│ Diagnóstico    │◄──────────────┤ (soporte TI     │
│ (análisis de   │               │  toma el caso)  │
│  causa raíz)   │               └────────────────┘
└──────┬─────────┘
       ▼
┌────────────────┐
│ Resolución     │  aplicar solución (rollback, reinicio, escalar tarea, fix BD)
└──────┬─────────┘
       ▼
┌────────────────┐
│ Validación     │  ¿alarma volvió a OK? ¿servicio estable?
└──────┬─────────┘
       ▼
┌────────────────┐
│ Cierre         │  documentar causa/solución → base de conocimiento
└──────┬─────────┘
       ▼
   (Fin)
```

### 3.2 Cada etapa BPMN → mecanismo real en SafeIndustrial

| Etapa BPMN | En SafeIndustrial (real) |
|---|---|
| Reporte / Identificación | Alarma CloudWatch entra en ALARM (o DLQ con mensajes, o reporte de usuario) |
| Registro | El correo SNS es el registro primario; se abre ticket con servicio, métrica, hora |
| Clasificación | Por el namespace de la alarma (AWS/ECS, AWS/RDS, AWS/AmazonMQ) y el servicio afectado |
| Priorización | Umbral + servicio: gateway/RDS = Crítica; backlog = Media; etc. |
| Escalamiento | SNS notifica al líder técnico; los críticos se escalan al PO |
| Diagnóstico | Logs en CloudWatch Logs (`/ecs/<servicio>`), métricas, último despliegue (`:sha`) |
| Resolución | Rollback (circuit breaker), reinicio de tarea, escalar tareas, optimizar query, reprocesar DLQ |
| Validación | La alarma vuelve a estado **OK**; el servicio queda estable |
| Cierre | Análisis post-incidente documentado (causa raíz + solución) en base de conocimiento |

---

## 4. Definición de Roles

### 4.1 Roles ITIL → equipo real

| Rol ITIL | Responsabilidad | Quién (SafeIndustrial) |
|---|---|---|
| **Usuario / Reporta** | Detecta y reporta el incidente | CloudWatch (automático), TRABAJADOR, cualquier usuario |
| **Mesa de ayuda / Registra** | Registra el ticket, primera línea | SNS + notification-service (registro automático) |
| **Soporte TI / Resuelve** | Diagnostica y aplica la solución | Equipo backend: **Imanol, Jeferson** |
| **Supervisor / Escala** | Decide escalamiento, coordina críticos | Líder técnico: **Ricardo Ismael** |
| **Gestor / Dueño del servicio** | Decisiones de negocio e impacto | Product Owner: **Mg. Ing. Rene Alonso Nieto Valencia** |
| **Administrador de plataforma** | Acceso AWS, infra, BD | `ADMINISTRADOR` (rol Keycloak) |

### 4.2 Roles RBAC reales del realm (contexto de acceso)

`ADMINISTRADOR, GERENCIA_GENERAL, JEFE_SEGURIDAD, LOGISTICA_ALMACEN, TRABAJADOR, MARKETING, INSTRUCTOR, ALUMNO` — el api-gateway aplica el mínimo privilegio por ruta/método (`SecurityConfig`).

---

## 5. Definición de SLA

SLA = acuerdo de tiempo de **respuesta** (alguien toma el caso) y de **resolución** (se restaura el servicio), por prioridad. Atados a los umbrales reales de las alarmas:

| Prioridad | Ejemplo | Tiempo de respuesta | Tiempo de resolución objetivo |
|---|---|---|---|
| 🔴 Crítica | gateway caído, RDS 98% CPU | ≤ 15 min | ≤ 1 hora |
| 🟠 Alta | login no funciona | ≤ 30 min | ≤ 4 horas |
| 🟡 Media | reportes erróneos, DLQ con mensajes | ≤ 4 horas | ≤ 1 día hábil |
| 🟢 Baja | lentitud puntual | ≤ 1 día hábil | ≤ 3 días hábiles |

> **Nota de diseño:** se corrige la tabla del PDF (que ponía tiempos menores a menor prioridad). Lo correcto es: **a mayor prioridad, menor tiempo permitido**.

**Disparador de SLA real:** el `period` y `evaluation-periods` de cada alarma definen qué tan rápido se nota el incidente. Ej.: las alarmas de CPU usan `period=300s, evaluationPeriods=1` → se detecta en ≤ 5 min, dentro del SLA de respuesta de un crítico.

---

## 6. Herramientas y Mejores Prácticas

### 6.1 Herramientas del PDF → stack real

| Herramienta del PDF | Función | Uso en SafeIndustrial |
|---|---|---|
| Zabbix / Grafana | Monitoreo / visualización | ✅ **Amazon CloudWatch** + Container Insights |
| (Alertas automáticas) | Notificación | ✅ **Amazon SNS** → email |
| Jira Service Management | Gestión de tickets | ✅ Jira (proyecto `SI`) — ya usado para solicitudes; extensible a incidentes |
| ServiceNow / Freshservice / Zendesk / GLPI | ITSM / mesa de ayuda | ❌ No usados; CloudWatch+SNS+Jira cubren el rol |
| (Resiliencia) | No perder eventos | ✅ RabbitMQ durable + **DLQ** |
| (Rollback) | Resolución automática | ✅ **Deployment Circuit Breaker** de ECS |

### 6.2 Alarmas reales configuradas (evidencia)

| Alarma | Métrica | Umbral | Acción |
|---|---|---|---|
| `ecs-cpu-<servicio>` (×7) | CPUUtilization | > 70% | → SNS |
| `ecs-ram-<servicio>` (×7) | MemoryUtilization | > 75% | → SNS |
| `rds-cpu-db-industrial-safety` | CPUUtilization | > 70% | → SNS |
| `rds-disco-db-industrial-safety` | FreeStorageSpace | < 2 GB | → SNS |
| `rds-ram-db-industrial-safety` | FreeableMemory | < 150 MB | → SNS |
| `rds-connections-high` *(en script)* | DatabaseConnections | > 315 | → SNS |
| `mq-backlog-<cola>` *(en script)* | MessageReadyCount | > 1000 | → SNS |
| `mq-dlq-<cola>` *(en script)* | MessageCount | > 0 | → SNS |

*(Las últimas tres se crean con `scripts/apply-capacity-plan.ps1 -Alarms`.)*

### 6.3 Mejores prácticas aplicadas

- ✅ **Detección 24/7 externa** (CloudWatch + SNS, independientes del cluster).
- ✅ **Registrar todo incidente** (correo SNS + posible ticket Jira).
- ✅ **Categorización** por namespace y servicio.
- ✅ **Automatizar alertas** (umbrales por servicio).
- ✅ **Resiliencia**: colas durables + DLQ (ningún evento se pierde).
- ✅ **Rollback automático** ante despliegues fallidos (circuit breaker).
- ✅ **Trazabilidad de cambios** (imagen por `:sha`, saber qué deploy causó el incidente).
- ⏳ Pendiente: base de conocimiento formal y análisis post-incidente sistemático.

---

## 7. Diagnóstico: implementado vs. pendiente

| Requisito del PDF | Estado | Nota |
|---|---|---|
| Identificación (monitoreo) | ✅ Implementado | 17 alarmas CloudWatch activas |
| Notificación / escalamiento | ✅ Implementado | SNS → email del líder |
| Clasificación por tipo | ✅ Modelado | namespace + servicio |
| Matriz de priorización | ✅ Definida | §2.3 |
| Resiliencia (no perder eventos) | ✅ Implementado | RabbitMQ durable + DLQ |
| Resolución automática (rollback) | ✅ Implementado | circuit breaker ECS |
| **Registro formal de tickets** | ⚠️ Parcial | hoy el correo SNS; falta abrir ticket Jira de incidente automáticamente |
| **SLA con seguimiento** | ⚠️ Parcial | umbrales definidos; falta medir tiempo real de resolución |
| **Base de conocimiento** | ❌ Pendiente | repositorio de incidentes + soluciones |
| **Análisis post-incidente** | ❌ Pendiente | post-mortem estructurado |
| **Registro de incidentes 24/7 independiente** | ❌ Pendiente | propuesta: SNS → Lambda → DynamoDB (§8) |

---

## 8. Plan de mejora

1. **Registro 24/7 independiente:** `SNS → Lambda → DynamoDB` para guardar cada incidente como ticket fuera del cluster (sobrevive a una caída total). Serverless, costo casi nulo.
2. **Ticket de incidente en Jira:** que la Lambda (o un webhook) abra automáticamente un ticket de incidente en Jira con servicio, métrica y severidad, reutilizando el `JiraClient` ya existente.
3. **SLA medible:** registrar `fechaDeteccion` y `fechaResolucion`; reporte de incidentes que incumplieron SLA.
4. **Base de conocimiento:** repositorio (wiki/Confluence/Markdown) con causa raíz + solución de cada incidente cerrado, para acelerar futuros diagnósticos.
5. **Post-mortem estructurado:** plantilla obligatoria para incidentes Críticos (qué pasó, causa raíz, cómo se evita la repetición).
6. **Cómo evitar la repetición:** autoscaling (CPU target) para incidentes de carga; read-replica RDS para incidentes de saturación de BD; healthchecks afinados (ver `Plan_Capacidad_Rendimiento_SafeIndustrial.md`).

---

## 9. Anexo — Evidencia y demo reproducible

### 9.1 Comandos de evidencia (solo lectura)

```bash
# Alarmas activas y su estado
aws cloudwatch describe-alarms --region us-east-1 \
  --query 'MetricAlarms[].{Name:AlarmName,Metric:MetricName,Threshold:Threshold,State:StateValue}' --output table

# Suscripción de notificación
aws sns list-subscriptions --region us-east-1 \
  --query 'Subscriptions[].{Topic:TopicArn,Protocol:Protocol,Endpoint:Endpoint}' --output table
```

### 9.2 Demo en vivo del flujo (sin necesidad de carga real)

Para **demostrar el flujo completo de notificación** ante el profe, se puede forzar una alarma a estado ALARM y verificar que llega el correo (incidente → notificación):

```bash
# Forzar la alarma del gateway a ALARM (dispara el correo SNS)
aws cloudwatch set-alarm-state \
  --alarm-name ecs-cpu-api-gateway-service-41zhurrw \
  --state-value ALARM \
  --state-reason "Demo gestión de incidentes — simulación de CPU alta" \
  --region us-east-1

# Devolverla a OK (cierre del incidente)
aws cloudwatch set-alarm-state \
  --alarm-name ecs-cpu-api-gateway-service-41zhurrw \
  --state-value OK \
  --state-reason "Demo — incidente resuelto" \
  --region us-east-1
```

> Esto demuestra **Identificación → Notificación → Cierre** con la herramienta real, sin gastar recursos ni levantar el entorno.

---

**Relacionado:** `Plan_Gestion_Solicitudes_SafeIndustrial.md`, `Plan_Capacidad_Rendimiento_SafeIndustrial.md`, `Autoscaling_ECS_SafeIndustrial.md`.
