# Handoff para continuar en la laptop (nueva sesión de Claude)

> **Cómo usar este archivo:** copia este contenido (o ábrelo) en una nueva sesión de Claude Code
> en tu laptop y pídele: *"Lee HANDOFF_LAPTOP.md y aplica/verifica lo pendiente"*.
> Contiene todo el contexto de lo que se hizo y lo que falta.

---

## 1. Contexto

- **Proyecto:** `industrial-security-backend` (microservicios Spring Boot 4 / Java 25).
- **AWS:** cuenta `160631388633`, región `us-east-1`, cluster ECS `industrial-safety-cluster`.
- **Descubrimiento:** producción usa **Cloud Map** (DNS interno), local usa Eureka. No mezclar.
- **Entrada pública:** túnel **ngrok** (`https://quartered-define-vibes.ngrok-free.dev`) → api-gateway.
- **Keycloak:** `http://industrial-safety.duckdns.org:8080`, realm `industrial-safety`, modo dev.

---

## 2. Qué se hizo en esta sesión

### 2.1 Commits YA pusheados a origin/main
- `d0eae39` fix(safety-service): mover contextLoads a IT + @DynamicPropertySource
- `f145036` fix(test): estabilizar ITs de chat-service y purchase-service en CI
- `5274fef` docs: guía de autoscaling
- `e8da791` eliminación del application.properties de purchase (corregía DB a localhost)
- `b8dc26c` fix(purchase): definir bean RestTemplate faltante (crash-loop / 500)

### 2.2 Commit local SIN pushear  ⚠️ ACCIÓN REQUERIDA
- `a35e4f6` **fix(gateway): puerto purchase 8087→8080 + inventario a GERENCIA_GENERAL**
  - Este fix ya se desplegó **manualmente** al gateway (task-def `api-gateway:13`), pero el
    commit **no está en origin**. Hay que **pushearlo** para que el repo/pipeline queden consistentes.

### 2.3 Archivos sin commitear (decidir si commitear)
- `load/carga.js`, `load/estres.js` — pruebas k6 de carga/estrés (todo hardcodeado, IP directa del gateway).
- `load/lt02-demo.js`, `load/lt01-login-keycloak.js`, `load/lt02-api-sync.js` — k6 con env vars.
- `scripts/fix-solicitudes-rabbitmq.ps1` — corrige host RabbitMQ + token JIRA de solicitudes.
- `scripts/prender-entorno.ps1` — encendido del entorno (para tarea programada).
- `scripts/scaling-policy-demo.json`, `scripts/_gen-guia.js` — auxiliares.
- `exam-service/Dockerfile` — modificado (revisar si el cambio es intencional).

### 2.4 Cambios aplicados directamente en AWS (NO en código)
- **Autoscaling ECS**: registrado en 7 servicios críticos (target tracking CPU). safety-service quedó con target 60%.
- **Alarmas CloudWatch** CPU/RAM/disco + tema SNS `safeindustrial-alarmas` → correo `ricardoismael777@gmail.com` (confirmar suscripción en el correo).
- **Dashboard** CloudWatch `SafeIndustrial-Consumo`.
- **IAM**: política `ECS-PassRole` agregada al rol `GitHubActions-IndustrialSafety` (necesaria para el pipeline nuevo).
- **Pipeline `deploy.yml`**: ahora registra task-def nueva con la imagen `:sha` (antes usaba force-new-deployment y no desplegaba código nuevo).
- **solicitudes-service**: task-def `:7` con host RabbitMQ corregido (`b-2451d61c...`) + `JIRA_API_TOKEN`.

---

## 3. TAREAS para esta sesión (lo que debes hacer / verificar)

1. **Pushear el fix del gateway:**
   ```powershell
   git push
   ```
   Verifica luego que CI pase y que el CD redespliegue (no rompe nada: ya estaba desplegado manual).

2. **(Opcional) Commitear los scripts útiles** (k6 + scripts ops) si los quieres en el repo:
   ```powershell
   git add load/carga.js load/estres.js scripts/fix-solicitudes-rabbitmq.ps1 scripts/prender-entorno.ps1
   git commit -m "chore: scripts de pruebas de carga y operacion"
   ```
   ⚠️ `load/carga.js` y `load/estres.js` tienen credenciales hardcodeadas — si no quieres secretos en el repo, NO los commitees (úsalos solo local).

3. **Verificar que el backend está sano** (ver comandos en sección 5).

---

## 4. Datos clave / gotchas

- **purchase estaba caído** por 2 bugs ya resueltos: (a) faltaba bean `RestTemplate` (crash→500); (b) el gateway apuntaba al puerto `8087` pero purchase escucha en `8080`. Ambos corregidos.
- **IPs públicas de Fargate cambian al reiniciar.** Dos efectos:
  - **Keycloak:** si reinicia, su IP cambia y hay que actualizar **DuckDNS** o el login se rompe app-wide.
    `https://www.duckdns.org/update?domains=industrial-safety&token=TU_TOKEN&ip=NUEVA_IP`
  - **Gateway:** los scripts k6 que pegan a la IP directa (`load/carga.js`, `estres.js`) tienen la IP hardcodeada; si cambia, actualizarla.
- **ngrok gratis throttlea** la carga → para estrés real, pegar a la IP pública del gateway en `:9000` (ya lo hacen carga.js/estres.js).
- **Amazon MQ** no se puede apagar (factura ~US$60/mes fijo). ECR cobra centavos.
- **Apagar el entorno al terminar** para no pagar: `./scripts/levantar-entorno.ps1 -Down` (keycloak se apaga aparte).

---

## 5. Comandos de verificación

```powershell
# Estado de todos los servicios
.\scripts\levantar-entorno.ps1 -Status

# IP actual del gateway (para los scripts k6 si cambió)
$t = aws ecs list-tasks --cluster industrial-safety-cluster --service-name api-gateway-service-41zhurrw --query "taskArns[0]" --output text --region us-east-1
$e = aws ecs describe-tasks --cluster industrial-safety-cluster --tasks $t --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value | [0]" --output text --region us-east-1
aws ec2 describe-network-interfaces --network-interface-ids $e --query "NetworkInterfaces[0].Association.PublicIp" --output text --region us-east-1

# Verificar IP de keycloak vs DuckDNS (deben coincidir; si no, actualizar DuckDNS)
# (mismo patrón, cambiando el service-name por industrial-safety-keycloak-task-service-l6iivm6a)

# Pruebas de carga / estrés (necesita k6 instalado)
k6 run load\carga.js
k6 run load\estres.js
```

### Estado confirmado al cierre de la sesión anterior
- Los 13 servicios ECS estaban **running 1/1**.
- `/api/v1/purchase/requests` con usuario gerente → **HTTP 200** (purchase OK vía gateway).
- `/api/v1/incidents` con admin → **HTTP 200**.
- Gateway en task-def `api-gateway:13` (con el fix del puerto).

---

## 6. Cambios EXACTOS de código (por si la laptop no los tiene en git)

> Si en la laptop el `git pull` no trae estos cambios (porque algún commit quedó solo en
> la otra PC), aplícalos manualmente. Son los fixes que ya están desplegados en AWS.

### 6.1 Gateway — puerto de purchase (commit `a35e4f6`, NO pusheado)
**Archivo:** `api-gateway/src/main/java/com/industrial/safety/api_gateway/config/GatewayConfig.java`
```diff
- private static final String PURCHASE_SERVICE  = "http://purchase-service.industrial-security:8087";
+ private static final String PURCHASE_SERVICE  = "http://purchase-service.industrial-security:8080";
```

### 6.2 Gateway — inventario accesible a gerencia (commit `a35e4f6`)
**Archivo:** `api-gateway/src/main/java/com/industrial/safety/api_gateway/config/SecurityConfig.java`
Después de la regla `GET /api/v1/purchase/requests ... hasAnyRole(LOGISTICA_ALMACEN, GERENCIA_GENERAL)`, agregar:
```java
// Gerencia tambien puede ver el inventario (dashboard de compras)
.pathMatchers(HttpMethod.GET, "/api/v1/purchase/inventory", "/api/v1/purchase/inventory/**").hasAnyRole(
        Role.LOGISTICA_ALMACEN.name(), Role.GERENCIA_GENERAL.name()
)
```

### 6.3 purchase — bean RestTemplate (commit `b8dc26c`, YA en origin)
**Archivo:** `purchase-service/src/main/java/com/logistica/purchase/config/RestTemplateConfig.java`
Agregar el bean (además del `RestClient` existente):
```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```
(con `import org.springframework.web.client.RestTemplate;`)

### 6.4 Cómo desplegar el gateway manualmente (rápido, sin esperar CI)
```powershell
# 1) compilar
cd api-gateway; .\mvnw.cmd package -DskipTests -q; cd ..
# 2) login + build + push a ECR
$reg="160631388633.dkr.ecr.us-east-1.amazonaws.com"; $repo="industrial-security/api-gateway"; $tag="fix-$(Get-Date -Format yyyyMMddHHmm)"
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $reg
docker build -t "${reg}/${repo}:$tag" -t "${reg}/${repo}:latest" api-gateway
docker push "${reg}/${repo}:$tag"; docker push "${reg}/${repo}:latest"
# 3) registrar task-def nueva con esa imagen y actualizar el servicio
#    (pídele a Claude que lo haga: describe-task-definition -> cambiar image -> register -> update-service)
```
> Nota: el login a ECR debe hacerse con el pipe en **Git Bash**, no PowerShell (PS corrompe el password).
