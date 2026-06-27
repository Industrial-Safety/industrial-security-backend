# Diseño — Módulo de Gestión de Incidencias TI (Fase 1)

**Proyecto:** SafeIndustrial (backend microservicios + frontend Next.js `industrial-safety-tech`)
**Marco de referencia:** ITIL 4 — Gestión de Incidentes (material S14-S27 del curso)
**Fecha:** 2026-06-26
**Alcance de este spec:** **Fase 1 — Núcleo**. La Fase 2 (sync externo con Freshservice + DLQ) queda documentada como fuera de alcance al final.

---

## 1. Objetivo

Permitir que **cualquier rol** reporte una **incidencia de TI** (algo de la plataforma que no funciona, relacionado a su área) mediante un formulario con evidencia, que el incidente se **registre en BD**, llegue al **Admin (rol TI)** en un módulo que los lista **por prioridad**, y que el Admin pueda **aceptar atenderlo** y **resolverlo** documentando cómo se solucionó. El reportero recibe **seguimiento y respuesta**.

Esto materializa, con herramientas reales, el Plan de Gestión de Incidentes del curso (Identificación → Registro → Clasificación → Priorización → Diagnóstico → Resolución → Validación → Cierre).

## 2. Aclaración de nombres (evitar choque)

- `incident` **ya existe** en el código y significa **infracción de seguridad** (detección por cámara, apelaciones). **No se toca.**
- Lo nuevo se llama **`incidencia` / Incidencias TI** y vive en su propio espacio. Nunca reusar el nombre `incident`.

## 3. Principios y restricciones (acordados con el usuario)

1. **Solo aditivo.** No se modifica código que ya funciona. Si reusar algo obliga a romperlo, se crea desde cero.
2. **Backend:** solo se agrega; se corren las pruebas. **El push al repositorio lo hace el usuario, no Claude.**
3. **YAGNI:** la Fase 1 cubre el 100% de las fases del entregable del curso. Lo demás es Fase 2.
4. Reusar lo que ya existe y es sano: **subida a S3**, **chat**, **bus RabbitMQ**, **notification-service**.

## 4. Decisión de arquitectura

**Nuevo microservicio `incidencias-service`** (mirror de `solicitudes-service`).

**Por qué (no un módulo dentro de un servicio existente):** es la opción con **riesgo cero** sobre el código que ya funciona (restricción #1), respeta el patrón de un microservicio por dominio, y mantiene Incidencias separado de Solicitudes (Jira) como pidió el usuario.

**Costo aceptado:** plomería nueva (módulo Maven, `application.yml`, ruta en el gateway, registro en discovery). Para la demo local no se requiere task-definition de AWS; se agrega después si se despliega.

## 5. Modelo de datos

Tabla nueva `incidencia` (PostgreSQL, esquema propio del servicio):

| Campo | Tipo | Notas |
|---|---|---|
| `id` | UUID | PK |
| `codigo` | String | Legible: `INC-2026-001` |
| `reporterId` | String | keycloakId del que reporta |
| `reporterName` | String | |
| `reporterRole` | String | rol normalizado (TRABAJADOR, INSTRUCTOR, …) |
| `categoria` | enum `Categoria` | INFRAESTRUCTURA, APLICACIONES, BASE_DATOS, REDES_COMUNICACIONES, SEGURIDAD, OTROS |
| `tipo` | String | subtipo según el rol (ej. "Video no carga") |
| `titulo` | String | requerido |
| `descripcion` | String (text) | requerido |
| `impacto` | enum `Nivel` | ALTO, MEDIO, BAJO |
| `urgencia` | enum `Nivel` | ALTO, MEDIO, BAJO |
| `prioridad` | enum `Prioridad` | **calculada** (CRITICA/ALTA/MEDIA/BAJA) — no se acepta del cliente |
| `evidenciaUrls` | List<String> | URLs S3 (tabla hija o columna JSON) |
| `estado` | enum `EstadoIncidencia` | REGISTRADO → EN_ATENCION → RESUELTO → CERRADO |
| `atendidoPor` | String (nullable) | keycloakId del Admin |
| `aceptadoEn` | Instant (nullable) | |
| `resolucionDescripcion` | String (nullable) | "cómo se solucionó" |
| `resueltoBien` | Boolean (nullable) | el "¿bien/mal?" del modal |
| `resueltoEn` | Instant (nullable) | |
| `conversationId` | String (nullable) | **opcional/reservado en Fase 1**: enlace a un hilo de chat para seguimiento. El seguimiento principal es la lista "mis incidencias" + notificación |
| `createdAt` / `updatedAt` | Instant | auditoría |

### 5.1 Cálculo de prioridad (matriz Impacto × Urgencia)

| Impacto \ Urgencia | ALTO | MEDIO | BAJO |
|---|---|---|---|
| **ALTO** | CRITICA | ALTA | MEDIA |
| **MEDIO** | ALTA | MEDIA | BAJA |
| **BAJO** | MEDIA | BAJA | BAJA |

Implementado en un componente puro `PrioridadCalculator` (testeable de forma aislada).

### 5.2 Transiciones de estado válidas

```
REGISTRADO --(admin acepta)--> EN_ATENCION --(admin resuelve)--> RESUELTO --(auto/cierre)--> CERRADO
```
Cualquier transición inválida → 409/400. (En Fase 1, RESUELTO puede pasar a CERRADO automáticamente tras la resolución; no se requiere validación manual del reportero.)

## 6. API (incidencias-service)

| Método | Ruta | Quién | Acción |
|---|---|---|---|
| POST | `/api/v1/incidencias` | cualquier rol autenticado | Crear (estado=REGISTRADO, prioridad calculada). Header `X-User-Id`=keycloakId |
| GET | `/api/v1/incidencias/mias` | reportero | Listar mis incidencias (seguimiento) |
| GET | `/api/v1/incidencias` | ADMINISTRADOR | Listar todas, **orden por prioridad** (CRITICA→BAJA) y luego fecha; filtros por `estado`/`prioridad` |
| GET | `/api/v1/incidencias/{id}` | ADMINISTRADOR o reportero dueño | Detalle |
| PATCH | `/api/v1/incidencias/{id}/aceptar` | ADMINISTRADOR | → EN_ATENCION, set `atendidoPor` |
| PATCH | `/api/v1/incidencias/{id}/resolver` | ADMINISTRADOR | body `{resolucionDescripcion, resueltoBien}` → RESUELTO/CERRADO |

- **Evidencia:** el frontend pide la URL prefirmada al endpoint **ya existente** `/api/v1/storage/upload-url`, sube a S3, y manda las URLs resultantes en el POST de la incidencia. `incidencias-service` solo las persiste.
- DTOs + MapStruct (`@Mapper(componentModel="spring")`), como en los otros servicios.

## 7. Mensajería (RabbitMQ) — reuso de notification-service

Al **crear** y al **resolver** una incidencia, `incidencias-service` publica un evento al exchange existente `industrial.safety.topic`.

- **Preferencia:** reusar las **routing keys/payload que `notification-service` ya consume** (`notification.email.queue`, `notification.ws.alert.queue`) para avisar al reportero por email + alerta WebSocket. Si el payload no calza, se **agrega** (no se modifica) un binding/cola nuevo en notification-service.
- Patrón idéntico a `solicitudes-service`: `RabbitMQConfig` propio + publisher.
- **La publicación es best-effort:** si RabbitMQ falla, la incidencia **igual queda guardada** en BD (la BD es la fuente de verdad); el fallo se loguea, no se pierde el registro.

## 8. Frontend (`industrial-safety-tech`) — todo aditivo

- **`src/services/incidenciaService.ts`** (NUEVO, separado de `incidentService.ts`): `crear`, `misIncidencias`, `listarTodas`, `aceptar`, `resolver`, `subirEvidencia` (reusa `/api/storage/upload-url`).
- **`<ReportarIncidenteModal>`** (NUEVO): formulario con
  - catálogo de `tipo` según el rol (objeto de configuración rol → tipos; ej. Alumno: curso/video/foro/pago; Instructor: subir video/curso; Trabajador: alertas/lentitud; Jefe: reportes/detecciones; Logística: solicitud de compra/EPP; Marketing: cambio de precio/cupones),
  - `titulo`, `descripcion`, selects `impacto` y `urgencia` con **preview de prioridad en vivo**,
  - subida de imágenes a S3.
- **Botón "Reportar incidente"** agregado en cada `src/app/*/support/page.tsx` (solo se inserta el botón + el modal; no se altera el chat existente).
- **`src/app/admin/incidencias/page.tsx`** (NUEVO): tabla ordenada por prioridad, con badges, filtros, botón **"Aceptar"** y botón **"Resolver"** que abre el **modal "¿cómo lo solucionaste? ¿quedó bien/mal?"**.
- **Seguimiento del reportero:** sección/página que lista "mis incidencias" con su estado (reusa el patrón de las otras listas del rol). La respuesta llega además por la notificación (email/WS).
- **Gateway:** agregar ruta `/api/v1/incidencias/**` → `incidencias-service` con RBAC (POST y `/mias` para cualquier rol autenticado; listar todas/aceptar/resolver solo ADMINISTRADOR).

## 9. Manejo de errores

- Validación de entrada (Bean Validation): `titulo`, `descripcion`, `impacto`, `urgencia` requeridos.
- `prioridad` **nunca** se acepta del cliente; siempre se calcula en el servidor.
- 404 si la incidencia no existe; 403 si un no-admin invoca endpoints de admin; 409/400 en transición de estado inválida.
- Fallo de publicación a RabbitMQ → log + la operación principal NO falla.
- Frontend: estados de carga, error y vacío en el modal y en el módulo admin.

## 10. Pruebas

- **Unitarias:** `PrioridadCalculator` (las 9 combinaciones Impacto×Urgencia), reglas de transición de estado, el mapper MapStruct.
- **Integración:** endpoints con **Testcontainers** (patrón existente del repo) + `@MockitoBean` para el `RabbitTemplate`/publisher.
- Cliente/efectos externos mockeados.

## 11. Qué se reusa vs. qué es nuevo

| Reusado (no se modifica) | Nuevo (aditivo) |
|---|---|
| `/api/v1/storage/upload-url` (S3) | microservicio `incidencias-service` |
| chat-service (hilo de seguimiento) | `incidenciaService.ts`, `<ReportarIncidenteModal>` |
| notification-service (consumo email/WS) | `admin/incidencias/page.tsx` |
| exchange `industrial.safety.topic` | botón en cada `*/support/page.tsx` |
| patrón solicitudes-service / Testcontainers | ruta de gateway `/incidencias/**` |

## 12. Explícitamente NO se toca

- El `incident` de seguridad (cámara/apelaciones) y su servicio.
- El envío/recepción de mensajes del chat existente.
- `solicitudes-service` y su integración con Jira.
- Los flujos actuales de notification-service (solo se agregan routing keys/bindings si hace falta).

## 13. Fuera de alcance (Fase 2, futura)

- Botón **"Sincronizar Freshservice"** + `FreshserviceClient` (REST `/api/v2/tickets`, API key en SSM).
- Sync **asíncrono** vía RabbitMQ (`incidencia.sync`) con **reintentos y DLQ** (`incidencia.sync.dlq`) para resiliencia ("ningún incidente se pierde aunque Freshservice esté caído").
- Persistir `freshserviceTicketId` y estado de sincronización en la entidad.

---

**Criterio de éxito Fase 1:** un usuario de cualquier rol reporta una incidencia con evidencia → aparece en el módulo del Admin ordenada por prioridad → el Admin la acepta y la resuelve con el modal → el reportero ve el estado y recibe notificación. Todo persistido en BD, sin romper nada existente.
