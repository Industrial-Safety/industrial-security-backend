# MATRIZ DE CASOS DE PRUEBA — SAFE INDUSTRIAL BACKEND
**Proyecto:** Industrial Safety Backend  
**Versión:** 1.0  
**Fecha:** 2026-05-18  
**Tecnologías:** Spring Boot 4.0.6 · Java 25 · JUnit 5 · Mockito · MockMvc · PostgreSQL

---

## LEYENDA

| Campo       | Valores posibles |
|-------------|-----------------|
| **Tipo**    | U = Unitaria · I = Integración |
| **Capa**    | SVC = Service · CTRL = Controller · REPO = Repository · COMP = Component |
| **Estado**  | Pendiente · Pasó · Falló |
| **Prioridad** | Alta · Media · Baja |

---

## 1. SAFETY-SERVICE

### 1.1 PpePointsCalculator (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| SS-U-01 | U | COMP | Lista null retorna 0 puntos | `totalDeduction()` | `null` | `0` | Alta | Pendiente |
| SS-U-02 | U | COMP | Lista vacía retorna 0 puntos | `totalDeduction()` | `[]` | `0` | Alta | Pendiente |
| SS-U-03 | U | COMP | Casco descuenta 20 puntos | `totalDeduction()` | `["Casco"]` | `20` | Alta | Pendiente |
| SS-U-04 | U | COMP | Guante descuenta 5 puntos | `totalDeduction()` | `["Guante"]` | `5` | Alta | Pendiente |
| SS-U-05 | U | COMP | Chaleco descuenta 10 puntos | `totalDeduction()` | `["Chaleco"]` | `10` | Alta | Pendiente |
| SS-U-06 | U | COMP | Vestimenta = alias de chaleco | `totalDeduction()` | `["Vestimenta"]` | `10` | Media | Pendiente |
| SS-U-07 | U | COMP | Casco + Guante + Chaleco suman 35 | `totalDeduction()` | `["Casco","Guante","Chaleco"]` | `35` | Alta | Pendiente |
| SS-U-08 | U | COMP | Tilde normalizada (Cásco = casco) | `totalDeduction()` | `["Cásco"]` | `20` | Media | Pendiente |
| SS-U-09 | U | COMP | Etiqueta desconocida retorna 0 | `totalDeduction()` | `["botas_seguridad"]` | `0` | Media | Pendiente |
| SS-U-10 | U | COMP | Mayúsculas normalizadas | `totalDeduction()` | `["CASCO"]` | `20` | Media | Pendiente |

### 1.2 ComplianceScoreServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| SS-U-11 | U | SVC | Deduce puntos de score existente | `applyDeduction()` | workerId, 20pts · score actual 100 | `80` | Alta | Pendiente |
| SS-U-12 | U | SVC | Crea score base si trabajador es nuevo | `applyDeduction()` | workerId nuevo · 20pts | `80` | Alta | Pendiente |
| SS-U-13 | U | SVC | Score no baja de 0 | `applyDeduction()` | score=10 · dedución=50 | `0` | Alta | Pendiente |
| SS-U-14 | U | SVC | Deducción 0 no cambia el score | `applyDeduction()` | score=75 · deducción=0 | `75` | Media | Pendiente |
| SS-U-15 | U | SVC | Restaura puntos sin superar base | `restorePoints()` | score=90 · base=100 · puntos=20 | `100` | Alta | Pendiente |
| SS-U-16 | U | SVC | Restaura parcialmente con espacio | `restorePoints()` | score=70 · base=100 · puntos=20 | `90` | Alta | Pendiente |
| SS-U-17 | U | SVC | Trabajador nuevo parte del base | `restorePoints()` | trabajador nuevo · base=100 | `100` | Media | Pendiente |
| SS-U-18 | U | SVC | Retorna score mapeado si existe | `getScore()` | workerId registrado | Score del repositorio | Alta | Pendiente |
| SS-U-19 | U | SVC | Retorna score base si no hay registro | `getScore()` | workerId nuevo | Score = 100 | Alta | Pendiente |

### 1.3 IncidentServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| SS-U-20 | U | SVC | Crea incidente con status PENDING | `create()` | CreateIncidentRequest válido | IncidentResponse con PENDING | Alta | Pendiente |
| SS-U-21 | U | SVC | Primera aprobación deduce puntos | `review()` | APPROVED + workerId | applyDeduction() llamado | Alta | Pendiente |
| SS-U-22 | U | SVC | Primera aprobación publica alerta | `review()` | APPROVED + workerId | publishPpeViolation() llamado | Alta | Pendiente |
| SS-U-23 | U | SVC | Aprobar sin workerId lanza exception | `review()` | APPROVED + workerId=null | IllegalArgumentException | Alta | Pendiente |
| SS-U-24 | U | SVC | Rechazar no deduce ni publica | `review()` | REJECTED | Sin interacciones con score/publisher | Alta | Pendiente |
| SS-U-25 | U | SVC | Re-aprobar no re-deduce | `review()` | APPROVED sobre APPROVED | Sin interacciones con score | Alta | Pendiente |
| SS-U-26 | U | SVC | Deducción 0 no publica alerta | `review()` | APPROVED + deducción=0 | Sin llamada a publisher | Media | Pendiente |
| SS-U-27 | U | SVC | Incidente no encontrado en review | `review()` | id inválido | EntityNotFoundException | Alta | Pendiente |
| SS-U-28 | U | SVC | list sin filtros invoca findAll | `list()` | null, null | Page con resultados | Alta | Pendiente |
| SS-U-29 | U | SVC | list por status invoca findByStatus | `list()` | PENDING, null | Page filtrada | Alta | Pendiente |
| SS-U-30 | U | SVC | list por cameraKey filtra correctamente | `list()` | null, "cam-0" | Page filtrada | Alta | Pendiente |
| SS-U-31 | U | SVC | list por ambos invoca query combinada | `list()` | PENDING, "cam-0" | Page filtrada | Alta | Pendiente |
| SS-U-32 | U | SVC | findById retorna si existe | `findById()` | id válido | IncidentResponse | Alta | Pendiente |
| SS-U-33 | U | SVC | findById lanza si no existe | `findById()` | id inválido | EntityNotFoundException | Alta | Pendiente |
| SS-U-34 | U | SVC | Apelación happy path | `submitAppeal()` | incidente APPROVED + mismo workerId | appealStatus=PENDING | Alta | Pendiente |
| SS-U-35 | U | SVC | Apelar incidente no APPROVED | `submitAppeal()` | PENDING incident | IllegalArgumentException | Alta | Pendiente |
| SS-U-36 | U | SVC | Apelar con worker incorrecto | `submitAppeal()` | otro workerId | IllegalArgumentException | Alta | Pendiente |
| SS-U-37 | U | SVC | Apelar con apelación ya PENDING | `submitAppeal()` | appealStatus=PENDING | IllegalArgumentException | Media | Pendiente |
| SS-U-38 | U | SVC | Resolver apelación aprobándola | `resolveAppeal()` | approved=true | APPEALED + restorePoints() | Alta | Pendiente |
| SS-U-39 | U | SVC | Resolver apelación rechazándola | `resolveAppeal()` | approved=false | REJECTED + sin restore | Alta | Pendiente |
| SS-U-40 | U | SVC | Resolver con reviewer incorrecto | `resolveAppeal()` | otro reviewerId | IllegalArgumentException | Alta | Pendiente |
| SS-U-41 | U | SVC | Resolver apelación no pendiente | `resolveAppeal()` | appealStatus=REJECTED | IllegalArgumentException | Alta | Pendiente |
| SS-U-42 | U | SVC | listAppeals onlyPending=true | `listAppeals()` | onlyPending=true | findByReviewedByAndAppealStatus | Media | Pendiente |
| SS-U-43 | U | SVC | listAppeals todas | `listAppeals()` | onlyPending=false | findByReviewedByAndAppealStatusIsNotNull | Media | Pendiente |

### 1.4 IncidentRepository (Integración)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| SS-I-01 | I | REPO | Filtra solo PENDING | `findByStatus()` | PENDING | 1 resultado | Alta | Pendiente |
| SS-I-02 | I | REPO | Filtra solo APPROVED | `findByStatus()` | APPROVED | 1 resultado | Alta | Pendiente |
| SS-I-03 | I | REPO | Estado inexistente retorna vacío | `findByStatus()` | REJECTED sin datos | 0 resultados | Media | Pendiente |
| SS-I-04 | I | REPO | Filtra por cámara correcta | `findByCameraKey()` | "cam-0" | 1 resultado | Alta | Pendiente |
| SS-I-05 | I | REPO | Cámara desconocida retorna vacío | `findByCameraKey()` | "cam-99" | 0 resultados | Media | Pendiente |
| SS-I-06 | I | REPO | Filtra por cámara y estado | `findByCameraKeyAndStatus()` | "cam-1" + APPROVED | 1 resultado | Alta | Pendiente |
| SS-I-07 | I | REPO | Sin coincidencia combinada retorna vacío | `findByCameraKeyAndStatus()` | "cam-0" + APPROVED | 0 resultados | Alta | Pendiente |
| SS-I-08 | I | REPO | Infracciones del trabajador | `findByWorkerId()` | workerId existente | 1 resultado | Alta | Pendiente |
| SS-I-09 | I | REPO | Trabajador sin infracciones | `findByWorkerId()` | workerId sin datos | 0 resultados | Media | Pendiente |
| SS-I-10 | I | REPO | Apelaciones PENDING del revisor | `findByReviewedByAndAppealStatus()` | PENDING | 1 resultado | Alta | Pendiente |
| SS-I-11 | I | REPO | Sin apelaciones PENDING retorna vacío | `findByReviewedByAndAppealStatus()` | PENDING sin datos | 0 resultados | Media | Pendiente |
| SS-I-12 | I | REPO | Todas las apelaciones del revisor | `findByReviewedByAndAppealStatusIsNotNull()` | reviewerId | 2 resultados | Alta | Pendiente |
| SS-I-13 | I | REPO | Excluye incidentes sin apelación | `findByReviewedByAndAppealStatusIsNotNull()` | appealStatus=null | 0 resultados | Alta | Pendiente |

### 1.5 IncidentController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | Datos de Entrada | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|------------------|-------------|-----------|--------|
| SS-I-20 | I | CTRL | Crear incidente válido | POST /incidents | payload completo | 201 + PENDING | Alta | Pendiente |
| SS-I-21 | I | CTRL | Crear sin campos obligatorios | POST /incidents | payload vacío | 400 | Alta | Pendiente |
| SS-I-22 | I | CTRL | Crear con confidence > 1.0 | POST /incidents | confidence=1.5 | 400 | Media | Pendiente |
| SS-I-23 | I | CTRL | Crear con violationTypes vacío | POST /incidents | violationTypes=[] | 400 | Media | Pendiente |
| SS-I-24 | I | CTRL | Obtener incidente existente | GET /incidents/{id} | id válido | 200 + body | Alta | Pendiente |
| SS-I-25 | I | CTRL | Obtener incidente inexistente | GET /incidents/{id} | id inválido | 404 | Alta | Pendiente |
| SS-I-26 | I | CTRL | Listar todos sin filtros | GET /incidents | — | 200 + page | Alta | Pendiente |
| SS-I-27 | I | CTRL | Listar por status PENDING | GET /incidents?status=PENDING | — | 200 + 1 item | Alta | Pendiente |
| SS-I-28 | I | CTRL | Listar por status APPROVED (vacío) | GET /incidents?status=APPROVED | — | 200 + 0 items | Media | Pendiente |
| SS-I-29 | I | CTRL | Listar por cámara | GET /incidents?cameraKey=cam-0 | — | 200 + 1 item | Alta | Pendiente |
| SS-I-30 | I | CTRL | Listar por cámara y estado | GET /incidents?status=PENDING&cameraKey=cam-0 | — | 200 + 1 item | Alta | Pendiente |
| SS-I-31 | I | CTRL | Infracciones propias — vacío | GET /incidents/mine | X-User-Id sin datos | 200 + 0 items | Alta | Pendiente |
| SS-I-32 | I | CTRL | Infracciones propias — con datos | GET /incidents/mine | X-User-Id con datos | 200 + 1 item | Alta | Pendiente |
| SS-I-33 | I | CTRL | Aprobar incidente con workerId | PATCH /incidents/{id}/review | APPROVED + workerId | 200 + APPROVED | Alta | Pendiente |
| SS-I-34 | I | CTRL | Rechazar incidente | PATCH /incidents/{id}/review | REJECTED | 200 + REJECTED | Alta | Pendiente |
| SS-I-35 | I | CTRL | Aprobar sin workerId | PATCH /incidents/{id}/review | APPROVED sin workerId | 400 | Alta | Pendiente |
| SS-I-36 | I | CTRL | Revisar sin campo status | PATCH /incidents/{id}/review | sin status | 400 | Media | Pendiente |
| SS-I-37 | I | CTRL | Revisar incidente inexistente | PATCH /incidents/{id}/review | id inválido | 404 | Alta | Pendiente |
| SS-I-38 | I | CTRL | Apelar infracción propia | POST /incidents/{id}/appeal | APPROVED + mismo worker | 200 + PENDING | Alta | Pendiente |
| SS-I-39 | I | CTRL | Apelar incidente no APPROVED | POST /incidents/{id}/appeal | PENDING incident | 400 | Alta | Pendiente |
| SS-I-40 | I | CTRL | Apelar con worker incorrecto | POST /incidents/{id}/appeal | otro worker | 400 | Alta | Pendiente |
| SS-I-41 | I | CTRL | Apelar sin campo reason | POST /incidents/{id}/appeal | reason ausente | 400 | Media | Pendiente |
| SS-I-42 | I | CTRL | Ver apelaciones del revisor | GET /incidents/appeals | X-User-Id revisor | 200 | Media | Pendiente |
| SS-I-43 | I | CTRL | Ver solo PENDING appeals | GET /incidents/appeals?onlyPending=true | con datos | 200 + 1 item | Media | Pendiente |
| SS-I-44 | I | CTRL | Resolver apelación — aprobada | PATCH /incidents/{id}/appeal/resolve | approved=true | 200 + APPEALED | Alta | Pendiente |
| SS-I-45 | I | CTRL | Resolver apelación — rechazada | PATCH /incidents/{id}/appeal/resolve | approved=false | 200 + REJECTED | Alta | Pendiente |
| SS-I-46 | I | CTRL | Resolver con reviewer incorrecto | PATCH /incidents/{id}/appeal/resolve | otro revisor | 400 | Alta | Pendiente |
| SS-I-47 | I | CTRL | Resolver sin campo approved | PATCH /incidents/{id}/appeal/resolve | sin approved | 400 | Media | Pendiente |

### 1.6 SafetyScoreController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | Datos de Entrada | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|------------------|-------------|-----------|--------|
| SS-I-50 | I | CTRL | Score de trabajador registrado | GET /safety-score/me | workerId con registro | 200 + score | Alta | Pendiente |
| SS-I-51 | I | CTRL | Score base para trabajador nuevo | GET /safety-score/me | workerId sin registro | 200 + score=100 | Alta | Pendiente |
| SS-I-52 | I | CTRL | Score reducido tras infracciones | GET /safety-score/me | workerId con deducción | 200 + score<100 | Alta | Pendiente |

---

## 2. USER-SERVICE

### 2.1 UserServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| US-U-01 | U | SVC | Crear usuario válido | `createUser()` | DTO válido | UserResponse creado | Alta | Pasó |
| US-U-02 | U | SVC | Crear usuario duplicado | `createUser()` | email existente | Excepción de conflicto | Alta | Pasó |
| US-U-03 | U | SVC | Obtener usuario por ID | `getUserById()` | id existente | UserResponse | Alta | Pasó |
| US-U-04 | U | SVC | ID no encontrado lanza exception | `getUserById()` | id inválido | ResourceNotFoundException | Alta | Pasó |
| US-U-05 | U | SVC | Actualizar datos del usuario | `updateUser()` | id + DTO | UserResponse actualizado | Alta | Pasó |
| US-U-06 | U | SVC | Eliminar usuario existente | `deleteUser()` | id existente | Sin excepción | Media | Pasó |
| US-U-07 | U | SVC | Generar QR para usuario | `generateQr()` | userId | QR bytes/url | Alta | Pasó |

### 2.2 UserController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| US-I-01 | I | CTRL | Crear usuario — happy path | POST /api/v1/users | 201 | Alta | Pasó |
| US-I-02 | I | CTRL | Crear usuario — payload inválido | POST /api/v1/users | 400 | Alta | Pasó |
| US-I-03 | I | CTRL | Obtener usuario existente | GET /api/v1/users/{id} | 200 | Alta | Pasó |
| US-I-04 | I | CTRL | Obtener usuario inexistente | GET /api/v1/users/{id} | 404 | Alta | Pasó |
| US-I-05 | I | CTRL | Actualizar usuario | PUT /api/v1/users/{id} | 200 | Media | Pasó |

### 2.3 UserRepository (Integración)

| ID | Tipo | Capa | Descripción | Método | Prioridad | Estado |
|----|------|------|-------------|--------|-----------|--------|
| US-I-10 | I | REPO | Buscar por email existente | `findByEmail()` | Alta | Pasó |
| US-I-11 | I | REPO | Buscar por email inexistente | `findByEmail()` | Alta | Pasó |

---

## 3. COURSE-SERVICE

### 3.1 CourseServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| CS-U-01 | U | SVC | Crear curso válido | `createCourse()` | CourseRequest | CourseResponse | Alta | Pasó |
| CS-U-02 | U | SVC | Obtener curso por ID existente | `getCourseById()` | id válido | CourseResponse | Alta | Pasó |
| CS-U-03 | U | SVC | Obtener curso inexistente | `getCourseById()` | id inválido | ResourceNotFoundException | Alta | Pasó |
| CS-U-04 | U | SVC | Listar todos los cursos | `getAllCourses()` | — | List<CourseResponse> | Media | Pasó |
| CS-U-05 | U | SVC | Actualizar curso existente | `updateCourse()` | id + DTO | CourseResponse actualizado | Alta | Pasó |
| CS-U-06 | U | SVC | Eliminar curso | `deleteCourse()` | id existente | Sin excepción | Media | Pasó |

### 3.2 CourseController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| CS-I-01 | I | CTRL | Crear curso — happy path | POST /api/v1/courses | 201 | Alta | Pasó |
| CS-I-02 | I | CTRL | Crear curso — payload inválido | POST /api/v1/courses | 400 | Alta | Pasó |
| CS-I-03 | I | CTRL | Obtener curso existente | GET /api/v1/courses/{id} | 200 | Alta | Pasó |
| CS-I-04 | I | CTRL | Obtener curso inexistente | GET /api/v1/courses/{id} | 404 | Alta | Pasó |
| CS-I-05 | I | CTRL | Listar todos los cursos | GET /api/v1/courses | 200 | Media | Pasó |

### 3.3 CourseRepository (Integración)

| ID | Tipo | Capa | Descripción | Método | Prioridad | Estado |
|----|------|------|-------------|--------|-----------|--------|
| CS-I-10 | I | REPO | Buscar cursos del instructor | `findByInstructorId()` | Alta | Pasó |
| CS-I-11 | I | REPO | Listar cursos publicados | `findByPublished()` | Alta | Pasó |

---

## 4. EXAM-SERVICE

### 4.1 ExamServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| ES-U-01 | U | SVC | Crear examen válido | `createExam()` | CreateExamRequest | ExamResponse | Alta | Pasó |
| ES-U-02 | U | SVC | Obtener examen por ID | `getExamById()` | id válido | ExamResponse | Alta | Pasó |
| ES-U-03 | U | SVC | Examen inexistente lanza exception | `getExamById()` | id inválido | ExamNotFoundException | Alta | Pasó |
| ES-U-04 | U | SVC | Listar exámenes del curso | `getExamsByCourse()` | courseId | List<ExamResponse> | Media | Pasó |

### 4.2 AttemptServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| ES-U-10 | U | SVC | Enviar intento válido | `submitAttempt()` | SubmitAttemptRequest | AttemptResultResponse | Alta | Pasó |
| ES-U-11 | U | SVC | Calificar respuestas correctamente | `submitAttempt()` | respuestas con correctas | score calculado | Alta | Pasó |
| ES-U-12 | U | SVC | Publicar evento ExamPassed al aprobar | `submitAttempt()` | score >= umbral | evento publicado | Alta | Pasó |
| ES-U-13 | U | SVC | No publicar evento si no aprueba | `submitAttempt()` | score < umbral | sin evento | Alta | Pasó |

### 4.3 ExamController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| ES-I-01 | I | CTRL | Crear examen — happy path | POST /api/v1/exams | 201 | Alta | Pasó |
| ES-I-02 | I | CTRL | Obtener examen existente | GET /api/v1/exams/{id} | 200 | Alta | Pasó |
| ES-I-03 | I | CTRL | Obtener examen inexistente | GET /api/v1/exams/{id} | 404 | Alta | Pasó |
| ES-I-04 | I | CTRL | Enviar intento | POST /api/v1/exams/{id}/attempt | 200 | Alta | Pasó |

---

## 5. ORDER-SERVICE

### 5.1 OrderServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| OS-U-01 | U | SVC | Crear orden sin cupón | `createOrder()` | OrderRequest sin cupón | OrderResponse PENDING | Alta | Pasó |
| OS-U-02 | U | SVC | Lista de ítems vacía lanza exception | `createOrder()` | ítems=[] | IllegalArgumentException | Alta | Pasó |
| OS-U-03 | U | SVC | Lista null lanza exception | `createOrder()` | ítems=null | IllegalArgumentException | Alta | Pasó |
| OS-U-04 | U | SVC | Total cero lanza exception | `createOrder()` | precio=0 | IllegalArgumentException | Alta | Pasó |
| OS-U-05 | U | SVC | Crear orden con cupón PERCENTAGE | `createOrder()` | cupón PERCENTAGE 10% | descuento aplicado | Alta | Pasó |
| OS-U-06 | U | SVC | Obtener orden por ID | `getOrderById()` | id existente | OrderResponse | Alta | Pasó |
| OS-U-07 | U | SVC | ID no encontrado lanza exception | `getOrderById()` | id inválido | ResourceNotFoundException | Alta | Pasó |
| OS-U-08 | U | SVC | Cancelar orden PENDING | `cancelOrder()` | PENDING | CANCELLED | Alta | Pasó |
| OS-U-09 | U | SVC | Cancelar COMPLETED lanza exception | `cancelOrder()` | COMPLETED | IllegalStateException | Alta | Pasó |
| OS-U-10 | U | SVC | Cancelar ya CANCELLED es idempotente | `cancelOrder()` | CANCELLED | Sin save | Alta | Pasó |
| OS-U-11 | U | SVC | PENDING → PROCESSING válido | `updateStatus()` | PENDING→PROCESSING | PROCESSING | Alta | Pasó |
| OS-U-12 | U | SVC | COMPLETED → PENDING ilegal | `updateStatus()` | COMPLETED→PENDING | IllegalStateException | Alta | Pasó |
| OS-U-13 | U | SVC | Pago exitoso → COMPLETED | `processPaymentResult()` | success=true | COMPLETED + notificaciones | Alta | Pasó |
| OS-U-14 | U | SVC | Pago fallido → FAILED | `processPaymentResult()` | success=false | FAILED + notificación fallo | Alta | Pasó |
| OS-U-15 | U | SVC | Pago en orden ya COMPLETED es idempotente | `processPaymentResult()` | ya COMPLETED | Sin save ni notif | Alta | Pasó |
| OS-U-16 | U | SVC | Pago exitoso con cupón consume uso | `processPaymentResult()` | success + cupón | couponService.consumeUse() | Alta | Pasó |

### 5.2 CouponServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Prioridad | Estado |
|----|------|------|-------------|--------|-----------|--------|
| OS-U-20 | U | SVC | Validar cupón activo y existente | `validateAndGet()` | Alta | Pasó |
| OS-U-21 | U | SVC | Cupón expirado lanza exception | `validateAndGet()` | Alta | Pasó |
| OS-U-22 | U | SVC | Cupón agotado lanza exception | `validateAndGet()` | Alta | Pasó |
| OS-U-23 | U | SVC | Consumir uso del cupón | `consumeUse()` | Alta | Pasó |

### 5.3 OrderController / CouponController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| OS-I-01 | I | CTRL | Crear orden válida | POST /api/v1/orders | 201 + PENDING | Alta | Pasó |
| OS-I-02 | I | CTRL | Crear orden — payload inválido | POST /api/v1/orders | 400 | Alta | Pasó |
| OS-I-03 | I | CTRL | Obtener orden por ID | GET /api/v1/orders/{id} | 200 | Alta | Pasó |
| OS-I-04 | I | CTRL | Orden inexistente | GET /api/v1/orders/{id} | 404 | Alta | Pasó |
| OS-I-05 | I | CTRL | Obtener por número de orden | GET /api/v1/orders/by-number/{num} | 200 | Alta | Pasó |
| OS-I-06 | I | CTRL | Número inexistente | GET /api/v1/orders/by-number/{num} | 404 | Alta | Pasó |
| OS-I-07 | I | CTRL | Órdenes del usuario | GET /api/v1/orders/by-user/{userId} | 200 | Alta | Pasó |
| OS-I-08 | I | CTRL | Usuario sin órdenes | GET /api/v1/orders/by-user/{userId} | 200 + [] | Media | Pasó |
| OS-I-09 | I | CTRL | Cancelar orden PENDING | DELETE /api/v1/orders/{id} | 204 | Alta | Pasó |
| OS-I-10 | I | CTRL | Cancelar orden inexistente | DELETE /api/v1/orders/{id} | 404 | Alta | Pasó |

### 5.4 OrderRepository (Integración)

| ID | Tipo | Capa | Descripción | Método | Prioridad | Estado |
|----|------|------|-------------|--------|-----------|--------|
| OS-I-20 | I | REPO | Buscar por userId | `findByUserId()` | Alta | Pasó |
| OS-I-21 | I | REPO | userId sin órdenes | `findByUserId()` | Alta | Pasó |
| OS-I-22 | I | REPO | Buscar por número | `findByOrderNumber()` | Alta | Pasó |
| OS-I-23 | I | REPO | Número inexistente | `findByOrderNumber()` | Alta | Pasó |
| OS-I-24 | I | REPO | Existe por número — true | `existsByOrderNumber()` | Alta | Pasó |
| OS-I-25 | I | REPO | Existe por número — false | `existsByOrderNumber()` | Alta | Pasó |
| OS-I-26 | I | REPO | Cursos solo COMPLETED | `findByLineItemCourseIdAndStatus()` | Alta | Pasó |
| OS-I-27 | I | REPO | PENDING no aparece en completados | `findByLineItemCourseIdAndStatus()` | Alta | Pasó |

---

## 6. PAYMENT-SERVICE

### 6.1 PaymentServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| PS-U-01 | U | SVC | Procesar pago exitoso | `processPayment()` | datos válidos | PaymentResponse SUCCESS | Alta | Pasó |
| PS-U-02 | U | SVC | Pago fallido por tarjeta | `processPayment()` | tarjeta inválida | PaymentResponse FAILED | Alta | Pasó |
| PS-U-03 | U | SVC | Obtener pago por ID | `getPaymentById()` | id existente | PaymentResponse | Alta | Pasó |
| PS-U-04 | U | SVC | ID no encontrado | `getPaymentById()` | id inválido | ResourceNotFoundException | Alta | Pasó |

### 6.2 PaymentController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| PS-I-01 | I | CTRL | Procesar pago — happy path | POST /api/v1/payments | 200/201 | Alta | Pasó |
| PS-I-02 | I | CTRL | Pago — payload inválido | POST /api/v1/payments | 400 | Alta | Pasó |
| PS-I-03 | I | CTRL | Obtener pago por ID | GET /api/v1/payments/{id} | 200 | Alta | Pasó |
| PS-I-04 | I | CTRL | Pago inexistente | GET /api/v1/payments/{id} | 404 | Alta | Pasó |

### 6.3 PaymentRepository (Integración)

| ID | Tipo | Capa | Descripción | Método | Prioridad | Estado |
|----|------|------|-------------|--------|-----------|--------|
| PS-I-10 | I | REPO | Buscar pago por número de orden | `findByOrderNumber()` | Alta | Pasó |
| PS-I-11 | I | REPO | Listar pagos por userId | `findByUserId()` | Media | Pasó |

---

## 7. CHAT-SERVICE

### 7.1 ChatServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| CH-U-01 | U | SVC | Crear conversación válida | `createConversation()` | ConversationRequest | ConversationResponse | Alta | Pasó |
| CH-U-02 | U | SVC | Enviar mensaje en conversación | `sendMessage()` | MessageRequest | MessageResponse | Alta | Pasó |
| CH-U-03 | U | SVC | Obtener conversación por ID | `getConversation()` | id existente | ConversationResponse | Alta | Pasó |
| CH-U-04 | U | SVC | Conversación no encontrada | `getConversation()` | id inválido | ResourceNotFoundException | Alta | Pasó |

### 7.2 ForumServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| CH-U-10 | U | SVC | Crear post de foro | `createPost()` | ForumPostRequest | ForumPostResponse | Alta | Pasó |
| CH-U-11 | U | SVC | Obtener post por ID | `getPostById()` | id existente | ForumPostResponse | Alta | Pasó |
| CH-U-12 | U | SVC | Post no encontrado | `getPostById()` | id inválido | ResourceNotFoundException | Alta | Pasó |
| CH-U-13 | U | SVC | Listar posts del curso | `getPostsByCourse()` | courseId | List<ForumPostResponse> | Alta | Pasó |

### 7.3 ChatController / ForumController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| CH-I-01 | I | CTRL | Crear conversación | POST /api/v1/conversations | 201 | Alta | Pasó |
| CH-I-02 | I | CTRL | Enviar mensaje | POST /api/v1/conversations/{id}/messages | 200 | Alta | Pasó |
| CH-I-03 | I | CTRL | Obtener conversación | GET /api/v1/conversations/{id} | 200 | Alta | Pasó |
| CH-I-04 | I | CTRL | Conversación inexistente | GET /api/v1/conversations/{id} | 404 | Alta | Pasó |
| CH-I-05 | I | CTRL | Crear post de foro | POST /api/v1/forum/posts | 201 | Alta | Pasó |
| CH-I-06 | I | CTRL | Listar posts del curso | GET /api/v1/forum/posts?courseId={id} | 200 | Alta | Pasó |

---

## 8. NOTIFICATION-SERVICE

### 8.1 EmailService / WebAlertService (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| NS-U-01 | U | SVC | Enviar email — happy path | `sendEmail()` | EmailEvent | email enviado sin excepción | Alta | Pasó |
| NS-U-02 | U | SVC | Email con destinatario nulo | `sendEmail()` | to=null | excepción controlada | Alta | Pasó |
| NS-U-03 | U | SVC | Publicar alerta WebSocket | `publishAlert()` | WebAlertEvent | template enviado | Alta | Pasó |

### 8.2 NotificationEventConsumer (Unitarias / Integración)

| ID | Tipo | Capa | Descripción | Método | Prioridad | Estado |
|----|------|------|-------------|--------|-----------|--------|
| NS-U-10 | U | SVC | Procesar evento de email | listener | Alta | Pasó |
| NS-U-11 | U | SVC | Procesar evento de alerta | listener | Alta | Pasó |
| NS-I-01 | I | SVC | Consumir evento real de RabbitMQ | listener integration | Alta | Pasó |

### 8.3 HealthController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| NS-I-10 | I | CTRL | Health check del servicio | GET /actuator/health | 200 | Alta | Pasó |

---

## 9. PURCHASE-SERVICE

### 9.1 InventoryServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| PU-U-01 | U | SVC | Retorna todos los ítems del inventario | `getAll()` | — | List con 1 ítem | Alta | Pendiente |
| PU-U-02 | U | SVC | Lista vacía si no hay ítems | `getAll()` | sin datos | [] | Media | Pendiente |
| PU-U-03 | U | SVC | Guarda ítem y retorna respuesta | `create()` | InventoryItemRequest | InventoryItemResponse | Alta | Pendiente |

### 9.2 PurchaseRequestServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| PU-U-10 | U | SVC | Retorna todas las solicitudes | `getAll()` | — | List con solicitudes | Alta | Pendiente |
| PU-U-11 | U | SVC | Lista vacía si no hay solicitudes | `getAll()` | sin datos | [] | Media | Pendiente |
| PU-U-12 | U | SVC | Retorna solicitud existente | `getById()` | id válido | PurchaseRequestResponse | Alta | Pendiente |
| PU-U-13 | U | SVC | Solicitud no encontrada lanza exception | `getById()` | id inválido | ResourceNotFoundException | Alta | Pendiente |
| PU-U-14 | U | SVC | Crea solicitud con estado PENDIENTE | `create()` | DTO válido | estado=PENDIENTE | Alta | Pendiente |
| PU-U-15 | U | SVC | Genera codigoSolicitud si no viene | `create()` | codigoSolicitud=null | código generado (SC-xxx) | Alta | Pendiente |
| PU-U-16 | U | SVC | No sobreescribe código existente | `create()` | código definido | código respetado | Media | Pendiente |
| PU-U-17 | U | SVC | Asigna fecha de hoy si no viene | `create()` | fecha=null | fecha=hoy | Media | Pendiente |
| PU-U-18 | U | SVC | Actualiza estado de solicitud | `updateStatus()` | id + "APROBADO" | estado=APROBADO | Alta | Pendiente |
| PU-U-19 | U | SVC | Actualizar solicitud inexistente lanza exception | `updateStatus()` | id inválido | ResourceNotFoundException | Alta | Pendiente |
| PU-U-20 | U | SVC | getStats cuenta correctamente | `getStats()` | 3 solicitudes | totales correctos | Alta | Pendiente |
| PU-U-21 | U | SVC | getStats retorna ceros si no hay datos | `getStats()` | sin datos | zeros | Media | Pendiente |
| PU-U-22 | U | SVC | getApproved retorna solo APROBADO | `getApproved()` | mix de estados | solo APROBADO | Alta | Pendiente |
| PU-U-23 | U | SVC | getApproved vacío si ninguna aprobada | `getApproved()` | solo PENDIENTE | [] | Media | Pendiente |

### 9.3 EppDeliveryServiceImpl (Unitarias)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| PU-U-30 | U | SVC | Entrega EPP y descuenta stock | `deliver()` | cantidad=3 · stock=10 | stock=7 + evento publicado | Alta | Pendiente |
| PU-U-31 | U | SVC | Stock insuficiente lanza exception | `deliver()` | cantidad=15 · stock=10 | InsufficientStockException | Alta | Pendiente |
| PU-U-32 | U | SVC | Stock cero lanza exception | `deliver()` | stock=0 | InsufficientStockException | Alta | Pendiente |
| PU-U-33 | U | SVC | Stock null se trata como 0 | `deliver()` | cantidad=null | InsufficientStockException | Media | Pendiente |
| PU-U-34 | U | SVC | Solicitud no encontrada lanza exception | `deliver()` | id inválido | ResourceNotFoundException | Alta | Pendiente |
| PU-U-35 | U | SVC | Entrega exacta deja stock en 0 | `deliver()` | cantidad=stock | stock=0 | Alta | Pendiente |
| PU-U-36 | U | SVC | Retorna entregas del trabajador | `getDeliveriesByWorkerDni()` | dni existente | List con entregas | Alta | Pendiente |
| PU-U-37 | U | SVC | Vacío si trabajador sin entregas | `getDeliveriesByWorkerDni()` | dni sin datos | [] | Media | Pendiente |

### 9.4 EppDeliveryRepository (Integración)

| ID | Tipo | Capa | Descripción | Método | Datos de Entrada | Resultado Esperado | Prioridad | Estado |
|----|------|------|-------------|--------|------------------|--------------------|-----------|--------|
| PU-I-01 | I | REPO | Retorna entregas del trabajador A | `findByWorkerDni()` | DNI_A | 2 resultados | Alta | Pendiente |
| PU-I-02 | I | REPO | Retorna solo entregas del trabajador B | `findByWorkerDni()` | DNI_B | 1 resultado | Alta | Pendiente |
| PU-I-03 | I | REPO | DNI desconocido retorna vacío | `findByWorkerDni()` | DNI inexistente | 0 resultados | Alta | Pendiente |
| PU-I-04 | I | REPO | Persistencia básica correcta | `save + findById()` | entidad válida | encontrado | Media | Pendiente |

### 9.5 PurchaseRequestController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| PU-I-10 | I | CTRL | Listar solicitudes | GET /requests | 200 + datos | Alta | Pendiente |
| PU-I-11 | I | CTRL | Obtener por ID existente | GET /requests/{id} | 200 | Alta | Pendiente |
| PU-I-12 | I | CTRL | Obtener por ID inexistente | GET /requests/{id} | 404 | Alta | Pendiente |
| PU-I-13 | I | CTRL | Crear solicitud válida | POST /requests | 201 + PENDIENTE | Alta | Pendiente |
| PU-I-14 | I | CTRL | Crear — genera código automático | POST /requests | 201 + código SC-xxx | Alta | Pendiente |
| PU-I-15 | I | CTRL | Crear — categoria vacía | POST /requests | 400 | Alta | Pendiente |
| PU-I-16 | I | CTRL | Crear — cantidad = 0 | POST /requests | 400 | Media | Pendiente |
| PU-I-17 | I | CTRL | Actualizar estado | PUT /requests/{id} | 200 + APROBADO | Alta | Pendiente |
| PU-I-18 | I | CTRL | Actualizar solicitud inexistente | PUT /requests/{id} | 404 | Alta | Pendiente |
| PU-I-19 | I | CTRL | Estadísticas del sistema | GET /requests/stats | 200 + totales | Alta | Pendiente |
| PU-I-20 | I | CTRL | Solicitudes aprobadas | GET /requests/inventory | 200 | Alta | Pendiente |
| PU-I-21 | I | CTRL | Aprobadas vacío si ninguna | GET /requests/inventory | 200 + [] | Media | Pendiente |

### 9.6 InventoryController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| PU-I-30 | I | CTRL | Listar inventario | GET /inventory | 200 + datos | Alta | Pendiente |
| PU-I-31 | I | CTRL | Inventario vacío | GET /inventory | 200 + [] | Media | Pendiente |
| PU-I-32 | I | CTRL | Crear ítem de inventario | POST /inventory | 201 | Alta | Pendiente |
| PU-I-33 | I | CTRL | Crear — codigo vacío | POST /inventory | 400 | Alta | Pendiente |
| PU-I-34 | I | CTRL | Crear — descripcion vacía | POST /inventory | 400 | Alta | Pendiente |
| PU-I-35 | I | CTRL | Crear — stock negativo | POST /inventory | 400 | Media | Pendiente |

### 9.7 EppDeliveryController (Integración)

| ID | Tipo | Capa | Descripción | Endpoint | HTTP Status | Prioridad | Estado |
|----|------|------|-------------|----------|-------------|-----------|--------|
| PU-I-40 | I | CTRL | Entrega EPP válida | POST /epp/deliver | 201 + entrega | Alta | Pendiente |
| PU-I-41 | I | CTRL | Stock insuficiente | POST /epp/deliver | 409 | Alta | Pendiente |
| PU-I-42 | I | CTRL | Campos faltantes | POST /epp/deliver | 400 | Alta | Pendiente |
| PU-I-43 | I | CTRL | Cantidad = 0 | POST /epp/deliver | 400 | Media | Pendiente |
| PU-I-44 | I | CTRL | Solicitud de compra inexistente | POST /epp/deliver | 404 | Alta | Pendiente |
| PU-I-45 | I | CTRL | Descuenta stock tras entrega | POST /epp/deliver | 201 + stock reducido | Alta | Pendiente |
| PU-I-46 | I | CTRL | Entregas del trabajador con datos | GET /epp/deliveries | 200 + datos | Alta | Pendiente |
| PU-I-47 | I | CTRL | Entregas — trabajador sin datos | GET /epp/deliveries | 200 + [] | Media | Pendiente |

---

## RESUMEN DE COBERTURA POR SERVICIO

| Servicio | Tests Unitarios | Tests Integración | Cobertura Objetivo | Estado |
|----------|-----------------|-------------------|--------------------|--------|
| safety-service | 43 casos | 34 casos | ≥ 80% | **Pendiente** |
| purchase-service | 28 casos | 22 casos | ≥ 80% | **Pendiente** |
| user-service | 7 casos | 5 casos | ≥ 80% | Implementado |
| course-service | 6 casos | 5 casos | ≥ 80% | Implementado |
| exam-service | 8 casos | 4 casos | ≥ 80% | Implementado |
| order-service | 16 casos | 10 casos | ≥ 80% | Implementado |
| payment-service | 4 casos | 4 casos | ≥ 80% | Implementado |
| chat-service | 8 casos | 6 casos | ≥ 80% | Implementado |
| notification-service | 4 casos | 2 casos | ≥ 80% | Implementado |
| **TOTAL** | **124 casos** | **92 casos** | — | — |

---

## CONVENCIONES DE EJECUCIÓN

```bash
# Unitarias solamente
mvn test -Dgroups="!integration"

# Integrales solamente (requiere Docker con PostgreSQL y RabbitMQ)
mvn test -Dgroups="integration"

# Todo el suite
mvn test

# Reporte de cobertura con JaCoCo
mvn verify
# Reporte en: target/site/jacoco/index.html
```

---

*Documento generado automáticamente a partir del análisis del repositorio industrial-security-backend.*
