# Plan de Gestión de Solicitudes de Servicio, Información y Acceso — SafeIndustrial

**Proyecto:** Plataforma de Gestión de Seguridad Industrial (SafeIndustrial)
**Marco:** ITIL 4 — Gestión de Peticiones de Servicio (Request Management) + ISO/IEC 20000
**Alcance:** procedimientos de Solicitudes de Servicio, Información y Acceso, aterrizados al sistema real.

---

## 1. Introducción

SafeIndustrial **ya implementa** un sistema de gestión de solicitudes distribuido. Este plan documenta los procedimientos ITIL apoyándose en los componentes reales del backend, identifica lo que está implementado y propone las mejoras pendientes.

**Componentes reales que intervienen:**

| Componente | Rol en la gestión de solicitudes | Evidencia |
|---|---|---|
| **solicitudes-service** | Registro central ITIL: recibe solicitudes de cualquier servicio, las clasifica (Servicio/Información/Acceso) y crea el ticket en Jira con trazabilidad | `Solicitud.java`, `SolicitudServiceImpl` |
| **Jira Service Management** | Herramienta de tickets: workflow de atención y cierre. Proyecto `SI` | `JiraClient`, `JiraProperties` |
| **notification-service** | Comunicación con el solicitante: email, certificados y alertas web (WebSocket) en cada etapa | `NotificationEventConsumer` (colas email/cert/ws-alert) |
| **Keycloak + api-gateway** | Gestión de Acceso (mínimo privilegio): autenticación y autorización por roles | `SecurityConfig` (RBAC), roles del realm |
| **RabbitMQ** | Bus de eventos: desacopla origen → registro → notificación, con colas durables y DLQ | `RabbitMQConfig` de cada servicio |
| **purchase-service** | Servicio origen: publica solicitudes (ej. compra de EPP por Logística) hacia el registro | `SolicitudEventPublisher` |
| **course-service** | Servicio origen: solicitudes de cambio de precio de cursos (creadas por Marketing) | `PriceChangeRequest`, `PriceChangeRequestController` |
| **user-service** | Ejecuta las solicitudes de ACCESO: crea usuarios y asigna/cambia roles en Keycloak | `KeycloakService.assignRole()`, `updateUserAdmin()` |

### 1.1 Arquitectura del flujo de solicitudes (vista lógica)

```
  Usuario / Servicio origen (ej. purchase)
        │  publica SolicitudCreatedEvent
        ▼
  RabbitMQ (exchange industrial.safety.topic, colas durables + DLQ)
        │
        ├──► solicitudes-service  ──►  crea ticket en JIRA (jiraKey: SI-xx)
        │        (registra tipo, origen, estado, prioridad, trazabilidad)
        │
        └──► notification-service ──►  email / alerta WebSocket / certificado
                                        al solicitante (confirmación, estado, entrega)

  Acceso a todo:  Keycloak (JWT) → api-gateway (RBAC por rol, mínimo privilegio)
```

---

## 2. Procedimiento 1 — Solicitudes de SERVICIO

**Definición:** petición de soporte, configuración o atención técnica (ej. compra/entrega de EPP, restablecer algo, una acción operativa).

**Flujo (etapas ITIL → implementación real):**

| Etapa ITIL | En SafeIndustrial |
|---|---|
| Recepción | El servicio origen publica `SolicitudCreatedEvent` a RabbitMQ |
| Registro | `solicitudes-service` guarda la `Solicitud` (estado `REGISTRADA`) |
| Validación / Atención | Se crea el ticket en Jira (estado `EN_JIRA`, `jiraKey`); el workflow de Jira gestiona la atención |
| Comunicación | `notification-service` envía email/alerta al solicitante |
| Entrega / Cierre | Se resuelve el ticket en Jira; notificación de cierre |

**Ejemplos reales:**
- **Compra de EPP** (origen Logística): purchase publica el evento → se registra y se abre ticket Jira → Gerencia aprueba/rechaza → notification avisa por correo.
- **Cambio de precio de curso** (origen Marketing): Marketing crea una solicitud para subir/bajar el precio de un curso (`/api/v1/course/price-requests`) → Gerencia aprueba/rechaza → notification avisa al solicitante. (`PriceChangeRequest`, `PriceChangeEventPublisher`).

---

## 3. Procedimiento 2 — Solicitudes de INFORMACIÓN

**Definición:** petición de datos o reportes (ej. reporte de compras, estado de servicios, datos de inventario).

**Flujo:** Recepción → Validación (¿el solicitante puede ver ese dato?) → Atención (consulta) → Entrega (reporte) → Cierre.

**En SafeIndustrial:**
- El tipo `INFORMACION` ya existe y se normaliza en `solicitudes-service` (`normalizarTipo()`).
- Endpoints de consulta/estadísticas ya disponibles (ej. `purchase/requests/stats`, dashboards).
- La **validación de quién puede ver qué** la aplica el gateway por rol (ver §5).

**Caso típico — reportes para Gerencia:** la Solicitud de Información se concreta **agregando/incrementando reportes accesibles al rol `GERENCIA_GENERAL`** (ej. reporte consolidado de compras, de incidentes, de cumplimiento). Cada reporte sensible que se consulte se registra como `tipo=INFORMACION` para dejar trazabilidad de **quién** pidió **qué** dato y **cuándo**.

**Estado:** el tipo está soportado; falta (a) agregar los reportes para Gerencia y (b) que se publiquen explícitamente como solicitudes `INFORMACION` para trazar cada consulta sensible (mejora §9).

---

## 4. Procedimiento 3 — Solicitudes de ACCESO

**Principio rector: mínimo privilegio.** Cada usuario solo accede a lo que su rol permite.

**Flujo ITIL:** Solicitud → Validación → Configuración → Verificación → Registro.

**En SafeIndustrial (control de acceso real):**
- **Autenticación:** Keycloak (realm `industrial-safety`), tokens JWT.
- **Autorización por rol** en el api-gateway (`SecurityConfig`), regla por ruta y método. Roles del realm:
  `ADMINISTRADOR, GERENCIA_GENERAL, JEFE_SEGURIDAD, LOGISTICA_ALMACEN, TRABAJADOR, MARKETING, INSTRUCTOR, ALUMNO`.
- **Ejemplos de mínimo privilegio ya aplicados:**
  - `/api/v1/purchase/requests` → solo `LOGISTICA_ALMACEN` o `GERENCIA_GENERAL`.
  - `/api/v1/incidents/mine` → solo `TRABAJADOR` (ve solo lo suyo).
  - `/api/v1/users/**` (gestión) → solo `ADMINISTRADOR`.

**Ejemplo del PDF — ascenso de Instructor a Jefe de Seguridad:**

Caso: un usuario con rol `INSTRUCTOR` (docente) asciende a `JEFE_SEGURIDAD`.

> **Decisión de diseño — depende de si sigue dando cursos:**
> - **Si deja de dictar (ascenso limpio):** se **reemplaza** el rol (quitar `INSTRUCTOR`, asignar `JEFE_SEGURIDAD`),
>   por mínimo privilegio. Sus cursos **no se borran** (el curso liga al profesor por ID + videos en S3) y los
>   **alumnos los siguen viendo** (lectura de cursos es pública). Si hay que editarlos, lo hace un `ADMINISTRADOR`
>   o se reasignan a otro instructor.
> - **Si sigue dictando además de ser jefe:** se **mantienen los 2 roles** (`INSTRUCTOR` + `JEFE_SEGURIDAD`),
>   porque quitar `INSTRUCTOR` le quitaría el acceso a **gestionar sus propios cursos** (editar, subir videos,
>   ver "mis cursos"). El gateway con `hasAnyRole` soporta la unión de permisos.
>
> **Importante:** quitar el rol nunca borra datos — cursos y videos persisten (ligados al ID del profesor y a S3),
> solo cambia qué puede **gestionar** el usuario.

**Flujo (Solicitud → Validación → Configuración → Verificación → Registro) y DÓNDE ocurre cada paso:**

| Etapa | Dónde ocurre |
|---|---|
| Solicitud | Se crea una solicitud `tipo=ACCESO` (ej. "ascender a jpérez a Jefe de Seguridad") |
| **Registro / Auditoría** | **solicitudes-service** la registra + **ticket en JIRA** (quién pidió, quién aprobó, cuándo) ← *aquí lo ve el profe* |
| Validación / Aprobación | `ADMINISTRADOR` aprueba en el ticket |
| **Configuración** | **user-service** ejecuta el cambio en Keycloak: `assignRole("JEFE_SEGURIDAD")` y quita el rol anterior (`updateUserAdmin`) ← *aquí se ejecuta* |
| Verificación | El usuario reloguea; su nuevo JWT trae `JEFE_SEGURIDAD`; el gateway ya lo deja entrar a las rutas de jefe |
| Notificación | notification-service avisa al usuario del cambio |

**Estado:** el **control** de acceso y la **ejecución del cambio de rol** ya existen (`KeycloakService.assignRole`). Falta **conectar el flujo como ticket de ACCESO** (que la solicitud de ascenso se registre en solicitudes-service + JIRA antes de ejecutarse) — mejora §9.

---

## 5. Plantilla de Solicitud (campos reales)

Basada en la entidad `Solicitud` (registro central):

| Campo | Significado | Ejemplo |
|---|---|---|
| `codigo` | Identificador legible de la solicitud | SOL-2026-014 |
| `tipo` | SERVICIO / INFORMACION / ACCESO | SERVICIO |
| `subtipo` | Detalle del tipo | "Compra de EPP" |
| `solicitante` | Quién solicita | jperez |
| `microservicioOrigen` | De dónde vino | purchase-service |
| `prioridad` | High / Medium / Low | Medium |
| `descripcion` | Detalle de la petición | "Solicitud de 10 cascos..." |
| `estado` | REGISTRADA / EN_JIRA / ERROR_JIRA | EN_JIRA |
| `jiraKey` | Ticket en Jira | SI-12 |
| `fechaSolicitud` / `fechaRegistro` | Trazabilidad temporal | 2026-06-15 |

---

## 6. Herramientas, Controles y Tickets

| Herramienta del PDF | Uso en SafeIndustrial |
|---|---|
| **Jira Service Management** | ✅ Tickets reales por cada solicitud (`jiraKey`), workflow de atención/cierre |
| **GitHub** | ✅ Control de versiones + CI/CD del backend |
| RabbitMQ | ✅ Bus de eventos con colas durables + **DLQ** (no se pierde ninguna solicitud) |
| Keycloak | ✅ Autenticación y control de acceso |
| (Alternativas del PDF) | ServiceNow, Freshdesk, Trello — no usadas; Jira cubre el rol |

---

## 7. Roles, SLA y Validación

**Roles (quién solicita / quién aprueba):**

| Rol | Puede solicitar / aprobar |
|---|---|
| MARKETING | Crear solicitudes de **cambio de precio de cursos** |
| LOGISTICA_ALMACEN | Crear solicitudes de **compra de EPP** |
| GERENCIA_GENERAL | Aprobar/rechazar solicitudes (compra, precio); ver **reportes** (Información) |
| TRABAJADOR | Apelar infracciones (solicitud), ver lo propio |
| JEFE_SEGURIDAD | Revisar incidentes y apelaciones |
| ADMINISTRADOR | Gestión de usuarios y **aprobar solicitudes de ACCESO** (cambios de rol) |

**SLA propuestos (por prioridad):**

| Prioridad | Tiempo de atención objetivo |
|---|---|
| High | 4 horas |
| Medium | 1 día hábil |
| Low | 3 días hábiles |

**Validación:** doble capa — el **gateway** valida el JWT y el rol antes de enrutar; cada servicio valida los datos de entrada (Bean Validation).

---

## 8. Diagnóstico: implementado vs. pendiente

| Requisito del PDF | Estado | Nota |
|---|---|---|
| Registro de solicitudes (tickets) | ✅ Implementado | solicitudes-service + Jira |
| Trazabilidad completa | ✅ Implementado | tipo, origen, estado, jiraKey, fechas |
| Tipos Servicio/Información/Acceso | ✅ Modelado | `normalizarTipo()` |
| Notificación al solicitante | ✅ Implementado | notification-service (email/ws/cert) |
| Control de acceso (mínimo privilegio) | ✅ Implementado | Keycloak + RBAC gateway |
| Resiliencia (no perder solicitudes) | ✅ Implementado | RabbitMQ durable + DLQ |
| **SLA / tiempos de atención** | ❌ Pendiente | falta `fechaLimite` por prioridad |
| **Ciclo de vida ITIL completo** | ⚠️ Parcial | estados técnicos en BD; el ciclo vive en Jira |
| **Flujo de Solicitud de Acceso (ticket)** | ❌ Pendiente | el acceso se asigna directo en Keycloak |
| **Solicitud de Información trazada** | ⚠️ Parcial | tipo soportado, sin publicador dedicado |
| **Aprobador en registro central** | ❌ Pendiente | falta campo `aprobador` + paso de aprobación |

---

## 9. Plan de mejoras (código) — siguiente fase

1. **SLA:** agregar `fechaLimite` a `Solicitud`, calculada según `prioridad`; alarma/reporte de solicitudes vencidas.
2. **Estados ITIL:** ampliar `SolicitudStatus` a `RECEPCION → VALIDACION → ATENCION → ENTREGA → CIERRE`, sincronizados con el workflow de Jira (webhook).
3. **Flujo de Solicitud de Acceso:** endpoint "solicitar rol" → publica `tipo=ACCESO` → ticket Jira → aprobación → asignación de rol en Keycloak (Admin API) → verificación → registro.
4. **Solicitud de Información trazada:** publicar `tipo=INFORMACION` cuando se piden reportes/datos sensibles, para dejar registro de cada acceso a información.
5. **Aprobador:** campo `aprobador` + paso de validación/aprobación en el registro central (unificar con la aprobación que ya hace purchase).

---

## 10. Buenas prácticas implementadas (resumen)

- ✅ **Todo se registra** (cada solicitud → ticket Jira + fila en BD).
- ✅ **Trazabilidad** de origen, estado y tiempos.
- ✅ **Seguridad y validación** en dos capas (gateway + servicio).
- ✅ **Roles claros** (solicitante / aprobador) vía Keycloak.
- ✅ **Resiliencia**: colas durables + DLQ (ninguna solicitud se pierde).
- ✅ **Desacoplamiento** por eventos (un servicio nuevo puede emitir solicitudes sin tocar el registro).
- ⏳ Pendiente formalizar **SLA** y el **ciclo de vida ITIL** en la entidad.
