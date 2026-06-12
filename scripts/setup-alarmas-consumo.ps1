<#
  setup-alarmas-consumo.ps1 — Alarmas de CONSUMO (CPU, RAM, Disco) -> correo
  Plan de Capacidad y Rendimiento (Fase 4 §5.2/§5.3).

  Cubre:
    - ECS Fargate (microservicios): CPU y RAM por servicio crítico
    - RDS PostgreSQL: CPU, RAM (FreeableMemory) y Disco (FreeStorageSpace)
  Notifica por SNS al correo que indiques.

  USO:
    ./setup-alarmas-consumo.ps1 -Email "tucorreo@gmail.com"
    ./setup-alarmas-consumo.ps1 -Email "tucorreo@gmail.com" -Test   # fuerza 1 alarma para probar el correo

  Requisitos: AWS CLI autenticada (cuenta 160631388633, us-east-1).
  NOTA: tras crear la suscripción, ABRE tu correo y haz clic en "Confirm subscription".
        Hasta que confirmes, AWS NO envía ninguna alarma.
#>
param(
  [Parameter(Mandatory = $true)][string]$Email,
  [switch]$Test,
  [string]$Cluster    = "industrial-safety-cluster",
  [string]$DbInstance = "db-industrial-safety",
  [string]$TopicName  = "safeindustrial-alarmas"
)

$ErrorActionPreference = "Stop"
$region = "us-east-1"

# Servicios críticos (nombres EXACTOS en ECS, con sufijo generado)
$criticalServices = @(
  "api-gateway-service-41zhurrw",
  "user-service-service-do5b3398",
  "safety-service-service-6r85geni",
  "solicitudes-service-service-xs6kjois",
  "order-service-service-rymvdlcl",
  "payment-service-service-4aky5hec",
  "purchase-service-service-ulonme67"
)

# ── Paso 1: Tema SNS + suscripción de correo ────────────────────────────────
Write-Host "== Paso 1: SNS topic + suscripción de $Email ==" -ForegroundColor Cyan
$topicArn = aws sns create-topic --name $TopicName --query 'TopicArn' --output text --region $region
aws sns subscribe --topic-arn $topicArn --protocol email --notification-endpoint $Email --region $region | Out-Null
Write-Host "Topic: $topicArn"
Write-Host ">> REVISA TU CORREO ($Email) y haz clic en 'Confirm subscription' <<" -ForegroundColor Yellow

# ── Paso 2: Alarmas ECS (CPU y RAM por servicio crítico) ────────────────────
Write-Host "== Paso 2: Alarmas ECS CPU y RAM ==" -ForegroundColor Cyan
foreach ($svc in $criticalServices) {
  # CPU > 70%
  aws cloudwatch put-metric-alarm --alarm-name "ecs-cpu-$svc" `
    --alarm-description "CPU > 70% en $svc" `
    --namespace AWS/ECS --metric-name CPUUtilization `
    --dimensions Name=ClusterName,Value=$Cluster Name=ServiceName,Value=$svc `
    --statistic Average --period 300 --evaluation-periods 1 `
    --threshold 70 --comparison-operator GreaterThanThreshold `
    --treat-missing-data notBreaching `
    --alarm-actions $topicArn --region $region
  # RAM > 75%
  aws cloudwatch put-metric-alarm --alarm-name "ecs-ram-$svc" `
    --alarm-description "RAM > 75% en $svc" `
    --namespace AWS/ECS --metric-name MemoryUtilization `
    --dimensions Name=ClusterName,Value=$Cluster Name=ServiceName,Value=$svc `
    --statistic Average --period 300 --evaluation-periods 1 `
    --threshold 75 --comparison-operator GreaterThanThreshold `
    --treat-missing-data notBreaching `
    --alarm-actions $topicArn --region $region
  Write-Host "  alarmas CPU+RAM: $svc"
}

# ── Paso 3: Alarmas RDS (CPU, RAM, Disco) ───────────────────────────────────
Write-Host "== Paso 3: Alarmas RDS CPU, RAM y Disco ==" -ForegroundColor Cyan
# CPU > 70%
aws cloudwatch put-metric-alarm --alarm-name "rds-cpu-$DbInstance" `
  --alarm-description "CPU > 70% en RDS $DbInstance" `
  --namespace AWS/RDS --metric-name CPUUtilization `
  --dimensions Name=DBInstanceIdentifier,Value=$DbInstance `
  --statistic Average --period 300 --evaluation-periods 1 `
  --threshold 70 --comparison-operator GreaterThanThreshold `
  --treat-missing-data notBreaching `
  --alarm-actions $topicArn --region $region
# RAM libre < 150 MB (157286400 bytes) -> poca memoria disponible
aws cloudwatch put-metric-alarm --alarm-name "rds-ram-$DbInstance" `
  --alarm-description "RAM libre < 150MB en RDS $DbInstance" `
  --namespace AWS/RDS --metric-name FreeableMemory `
  --dimensions Name=DBInstanceIdentifier,Value=$DbInstance `
  --statistic Average --period 300 --evaluation-periods 1 `
  --threshold 157286400 --comparison-operator LessThanThreshold `
  --treat-missing-data notBreaching `
  --alarm-actions $topicArn --region $region
# Disco libre < 2 GB (2147483648 bytes) -> de 20GB totales
aws cloudwatch put-metric-alarm --alarm-name "rds-disco-$DbInstance" `
  --alarm-description "Disco libre < 2GB en RDS $DbInstance" `
  --namespace AWS/RDS --metric-name FreeStorageSpace `
  --dimensions Name=DBInstanceIdentifier,Value=$DbInstance `
  --statistic Average --period 300 --evaluation-periods 1 `
  --threshold 2147483648 --comparison-operator LessThanThreshold `
  --treat-missing-data notBreaching `
  --alarm-actions $topicArn --region $region
Write-Host "  alarmas RDS CPU+RAM+Disco: $DbInstance"

# ── Paso 4 (opcional): forzar una alarma para probar que el correo llega ─────
if ($Test) {
  Write-Host "== Paso 4: Forzando 'rds-cpu-$DbInstance' a ALARM para probar el correo ==" -ForegroundColor Cyan
  Write-Host "(Solo funciona si YA confirmaste la suscripción en tu correo)" -ForegroundColor Yellow
  aws cloudwatch set-alarm-state --alarm-name "rds-cpu-$DbInstance" `
    --state-value ALARM --state-reason "Prueba manual de notificación por correo" --region $region
  Write-Host ">> Debe llegarte un correo de la alarma en 1-2 min. Luego vuelve sola a OK/INSUFFICIENT_DATA."
}

Write-Host ""
Write-Host "Listo. Ver alarmas:  aws cloudwatch describe-alarms --query 'MetricAlarms[].{Nombre:AlarmName,Estado:StateValue}' --output table" -ForegroundColor Green
