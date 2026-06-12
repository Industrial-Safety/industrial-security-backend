# Cómo funciona el Autoscaling — SafeIndustrial (ECS Fargate)

**Aplicado en:** cuenta AWS 160631388633 · región us-east-1 · cluster `industrial-safety-cluster`
**Referencia del plan:** `Plan_Capacidad_Rendimiento_SafeIndustrial.md` §4.1

---

## 1. Qué es

El autoscaling hace que cada microservicio **agregue o quite tareas (contenedores) solo**, según
la carga, sin que nadie lo toque a mano. Se usa **AWS Application Auto Scaling** con una política de
**Target Tracking** (seguimiento de objetivo).

## 2. Cómo funciona (en una frase)

> "Mantén la CPU promedio del servicio en **60%**. Si sube de ahí, crea tareas; si baja, quítalas."

```
   Carga sube  ─►  CPU > 60%  ─►  AWS crea tareas (hasta 4)  ─►  CPU vuelve a ~60%
   Carga baja  ─►  CPU < 60%  ─►  AWS quita tareas (hasta 1)  ─►  ahorro de costo
```

AWS crea por debajo unas **alarmas CloudWatch automáticas** (no se tocan) que disparan el escalado.

## 3. Las 2 piezas que se configuran

| Pieza | Qué define | Valor aplicado |
|-------|-----------|----------------|
| **Scalable target** | El rango permitido de tareas | `min 1` · `max 4` |
| **Scaling policy** (Target Tracking) | La regla de cuándo escalar | CPU promedio **60%** |
| Cooldowns | Anti-rebote | subir **60s** · bajar **300s** |

El cooldown de bajada (300s) es más largo que el de subida (60s): así reacciona **rápido** ante un pico
pero baja **con calma**, evitando el efecto yo-yo (crear/borrar tareas sin parar).

## 4. Servicios con autoscaling (los 7 críticos)

api-gateway · user-service · safety-service · solicitudes-service · order-service · payment-service · purchase-service

(Los secundarios —course, exam, chat, notification— quedan sin autoscaling por menor criticidad.)

## 5. Cómo se configuró (comandos reales)

```bash
# Pieza 1: el rango (min 1, max 4) — por cada servicio crítico
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/industrial-safety-cluster/safety-service-service-6r85geni \
  --min-capacity 1 --max-capacity 4

# Pieza 2: la regla (CPU objetivo 60%)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/industrial-safety-cluster/safety-service-service-6r85geni \
  --policy-name cpu-target-60 --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration file://scripts/scaling-policy.json
```

`scripts/scaling-policy.json`:
```json
{
  "TargetValue": 60.0,
  "PredefinedMetricSpecification": { "PredefinedMetricType": "ECSServiceAverageCPUUtilization" },
  "ScaleOutCooldown": 60,
  "ScaleInCooldown": 300
}
```

> Reproducible con: `./scripts/apply-capacity-plan.ps1 -AutoScaling`

## 6. Cómo demostrarlo (ver el escalado en vivo)

```powershell
# 1. Entorno encendido y sano
.\scripts\levantar-entorno.ps1 -Status

# 2. Generar carga -> la CPU sube de 60% -> AWS crea tareas
k6 run -e GATEWAY_URL=https://quartered-define-vibes.ngrok-free.dev `
       -e KEYCLOAK_URL=<tu-keycloak> -e CLIENT_ID=web `
       -e TEST_USER=demo -e TEST_PASS=demo load/lt02-api-sync.js

# 3. Ver el conteo de tareas subir de 1 -> 2 -> 3 -> 4
aws ecs describe-services --cluster industrial-safety-cluster `
  --services safety-service-service-6r85geni `
  --query "services[0].{Desired:desiredCount,Running:runningCount}" --output table
```

En la consola: **CloudWatch → Dashboards → SafeIndustrial-Consumo** (CPU/RAM) y
**ECS → cluster → servicio → Events** muestra los mensajes de escalado.

## 7. Cómo verificar el estado del autoscaling

```bash
aws application-autoscaling describe-scalable-targets --service-namespace ecs \
  --query "ScalableTargets[].{Servicio:ResourceId,Min:MinCapacity,Max:MaxCapacity}" --output table
```

## 8. Cómo apagarlo (deja de escalar / dejar de pagar)

```powershell
.\scripts\levantar-entorno.ps1 -Down   # des-registra los targets y baja todo a 0
```

Con `min 1`, un simple `desired-count 0` NO se mantiene (el autoscaling re-sube a 1);
por eso `-Down` primero **des-registra el scalable target**.
