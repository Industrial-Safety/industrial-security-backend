<#
  levantar-entorno.ps1 — Enciende / apaga el entorno ECS + RDS para la demo.

  El pipeline de GitHub (deploy.yml) ya hace build->ECR->update-service, pero
  con desiredCount=0 no arranca nada. Este script sube/baja el desiredCount
  para encender (y ver métricas) o apagar (y dejar de facturar).

  USO:
    ./levantar-entorno.ps1 -Up      # arranca RDS + pone los servicios en 1 tarea
    ./levantar-entorno.ps1 -Down    # pone todo en 0 + detiene la RDS (ahorra costo)
    ./levantar-entorno.ps1 -Status  # muestra estado actual

  Requisitos: AWS CLI autenticada (cuenta 160631388633, us-east-1).
  NOTA: course-service y chat-service usan MongoDB; si no hay Mongo provisionado
        en AWS pueden quedar en reinicio. Para la demo de CPU/RAM, los servicios
        con RDS (user, safety, order, payment, purchase, solicitudes, exam) bastan.
#>
param(
  [switch]$Up,
  [switch]$Down,
  [switch]$Status,
  [string]$Cluster    = "industrial-safety-cluster",
  [string]$DbInstance = "db-industrial-safety",
  [int]$Tasks         = 1
)

$ErrorActionPreference = "Stop"
$region = "us-east-1"

$keycloak = "industrial-safety-keycloak-task-service-l6iivm6a"
$services = @(
  "api-gateway-service-41zhurrw",
  "user-service-service-do5b3398",
  "safety-service-service-6r85geni",
  "solicitudes-service-service-xs6kjois",
  "order-service-service-rymvdlcl",
  "payment-service-service-4aky5hec",
  "purchase-service-service-ulonme67",
  "exam-service-service-2nw0ujch",
  "notification-service-service-jxfxyfhi",
  "course-service-service-ifmrw0rc",
  "chat-service-service-xf2hmld7"
)

if (-not ($Up -or $Down -or $Status)) {
  Write-Host "Indica una acción: -Up | -Down | -Status"
  exit 1
}

if ($Status) {
  Write-Host "== Estado RDS ==" -ForegroundColor Cyan
  aws rds describe-db-instances --db-instance-identifier $DbInstance `
    --query 'DBInstances[0].DBInstanceStatus' --output text --region $region
  Write-Host "== Servicios ECS (desired/running) ==" -ForegroundColor Cyan
  $all = @($keycloak) + $services
  foreach ($svc in $all) {
    aws ecs describe-services --cluster $Cluster --services $svc `
      --query 'services[0].{Servicio:serviceName,Desired:desiredCount,Running:runningCount}' `
      --output text --region $region
  }
  exit 0
}

if ($Up) {
  # 1. RDS
  Write-Host "== 1) Arrancando RDS $DbInstance ==" -ForegroundColor Cyan
  $state = aws rds describe-db-instances --db-instance-identifier $DbInstance `
    --query 'DBInstances[0].DBInstanceStatus' --output text --region $region
  if ($state -eq "stopped") {
    aws rds start-db-instance --db-instance-identifier $DbInstance --region $region | Out-Null
    Write-Host "Esperando a que la RDS esté 'available' (5-10 min)..."
    aws rds wait db-instance-available --db-instance-identifier $DbInstance --region $region
    Write-Host "RDS lista." -ForegroundColor Green
  } else {
    Write-Host "RDS ya está en estado '$state' (no se toca)."
  }

  # 2. Keycloak primero
  Write-Host "== 2) Levantando Keycloak ==" -ForegroundColor Cyan
  aws ecs update-service --cluster $Cluster --service $keycloak `
    --desired-count $Tasks --region $region --query 'service.serviceName' --output text

  # 3. Microservicios
  Write-Host "== 3) Levantando microservicios (desired=$Tasks) ==" -ForegroundColor Cyan
  foreach ($svc in $services) {
    aws ecs update-service --cluster $Cluster --service $svc `
      --desired-count $Tasks --region $region --query 'service.serviceName' --output text
  }
  Write-Host ""
  Write-Host "Listo. En ~3-5 min las tareas quedan RUNNING y Container Insights empieza a publicar CPU/RAM." -ForegroundColor Green
  Write-Host "Verifica con: ./levantar-entorno.ps1 -Status"
}

if ($Down) {
  # Quitar el autoscaling primero: con min>=1 el target re-sube las tareas y
  # un 'desired-count 0' no se mantiene. Hay que des-registrar el scalable target.
  Write-Host "== Quitando autoscaling (des-registrar scalable targets) ==" -ForegroundColor Cyan
  $critical = @(
    "api-gateway-service-41zhurrw","user-service-service-do5b3398","safety-service-service-6r85geni",
    "solicitudes-service-service-xs6kjois","order-service-service-rymvdlcl",
    "payment-service-service-4aky5hec","purchase-service-service-ulonme67"
  )
  foreach ($svc in $critical) {
    try {
      aws application-autoscaling deregister-scalable-target --service-namespace ecs `
        --scalable-dimension ecs:service:DesiredCount `
        --resource-id "service/$Cluster/$svc" --region $region 2>$null
    } catch {}
  }

  Write-Host "== Apagando microservicios y Keycloak (desired=0) ==" -ForegroundColor Cyan
  foreach ($svc in ($services + $keycloak)) {
    aws ecs update-service --cluster $Cluster --service $svc `
      --desired-count 0 --region $region --query 'service.serviceName' --output text
  }
  Write-Host "== Deteniendo RDS $DbInstance ==" -ForegroundColor Cyan
  $state = aws rds describe-db-instances --db-instance-identifier $DbInstance `
    --query 'DBInstances[0].DBInstanceStatus' --output text --region $region
  if ($state -eq "available") {
    aws rds stop-db-instance --db-instance-identifier $DbInstance --region $region | Out-Null
    Write-Host "RDS deteniéndose."
  } else {
    Write-Host "RDS en estado '$state' (no se detiene)."
  }
  Write-Host "Entorno apagado. (Recuerda: Amazon MQ sigue facturando; no se puede detener.)" -ForegroundColor Yellow
}
