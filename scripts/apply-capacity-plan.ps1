<#
  apply-capacity-plan.ps1 — Aplica las acciones AWS del Plan de Capacidad y Rendimiento
  (ver Plan_Capacidad_Rendimiento_SafeIndustrial.md, Fases 3 y 4).

  Requisitos: AWS CLI autenticada contra la cuenta 160631388633 (us-east-1).

  Cada sección es independiente. Ejecutar SOLO lo que se decida:

    ./apply-capacity-plan.ps1 -CircuitBreaker   # GRATIS: rollback automático de despliegues ECS
    ./apply-capacity-plan.ps1 -Alarms           # ~US$5-10/mes: SNS + alarmas (CPU, RDS, backlog MQ, DLQs)
    ./apply-capacity-plan.ps1 -Rds              # COSTO ~+US$70-85/mes: t4g.micro -> t4g.medium Multi-AZ + gp3
    ./apply-capacity-plan.ps1 -AutoScaling      # registra autoscaling. OJO: con MinTasks>=1 ENCIENDE tareas (factura)

  Orden recomendado por el plan: CircuitBreaker y Alarms cuando quieras (no facturan tareas);
  Rds ANTES de AutoScaling (ver §2.3 del plan: no escalar sin resolver conexiones).
#>
param(
  [switch]$Rds,
  [switch]$ReadReplica,
  [switch]$AutoScaling,
  [switch]$CircuitBreaker,
  [switch]$Alarms,
  [string]$Cluster = "industrial-safety-cluster",
  [string]$DbInstance = "db-industrial-safety",
  [string]$DbClass = "db.t4g.medium",
  [string]$ReplicaId = "db-industrial-safety-replica",
  [string]$BrokerName = "industrial-security-rabbitmq",
  [string]$AlarmEmail = "ricardoismael777@gmail.com",
  [int]$MinTasks = 2,
  [int]$MaxTasks = 6
)

$ErrorActionPreference = "Stop"
$region = "us-east-1"

# Nombres EXACTOS de los servicios en el cluster (incluyen sufijo generado por ECS)
$criticalServices = @(
  "api-gateway-service-41zhurrw",
  "user-service-service-do5b3398",
  "safety-service-service-6r85geni",
  "solicitudes-service-service-xs6kjois",
  "order-service-service-rymvdlcl",
  "payment-service-service-4aky5hec",
  "purchase-service-service-ulonme67"
)
$secondaryServices = @(
  "course-service-service-ifmrw0rc",
  "exam-service-service-2nw0ujch",
  "chat-service-service-xf2hmld7",
  "notification-service-service-jxfxyfhi"
)
$allServices = $criticalServices + $secondaryServices

if (-not ($Rds -or $ReadReplica -or $AutoScaling -or $CircuitBreaker -or $Alarms)) {
  Write-Host "Indica al menos una sección: -CircuitBreaker | -Alarms | -Rds | -ReadReplica | -AutoScaling"
  Write-Host "Lee el encabezado de este script para costos y orden recomendado."
  exit 1
}

# ─────────────────────────────────────────────────────────────────────────────
# Rollback automático de despliegues (Fase 3 §4.1) — gratis
# ─────────────────────────────────────────────────────────────────────────────
if ($CircuitBreaker) {
  Write-Host "== Circuit breaker de despliegue + minimumHealthyPercent=100 en $($allServices.Count) servicios =="
  foreach ($svc in $allServices) {
    aws ecs update-service --cluster $Cluster --service $svc `
      --deployment-configuration "deploymentCircuitBreaker={enable=true,rollback=true},maximumPercent=200,minimumHealthyPercent=100" `
      --region $region --query 'service.serviceName' --output text
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# Alarmas proactivas (Fase 4 §5.3) — ~US$0.10/alarma/mes
# ─────────────────────────────────────────────────────────────────────────────
if ($Alarms) {
  Write-Host "== SNS + alarmas CloudWatch =="
  $topicArn = aws sns create-topic --name safeindustrial-alarms --query 'TopicArn' --output text
  aws sns subscribe --topic-arn $topicArn --protocol email --notification-endpoint $AlarmEmail | Out-Null
  Write-Host "Topic: $topicArn — CONFIRMA la suscripción en el correo $AlarmEmail"

  foreach ($svc in $criticalServices) {
    aws cloudwatch put-metric-alarm --alarm-name "ecs-cpu-high-$svc" `
      --namespace AWS/ECS --metric-name CPUUtilization `
      --dimensions Name=ClusterName,Value=$Cluster Name=ServiceName,Value=$svc `
      --statistic Average --period 300 --evaluation-periods 1 `
      --threshold 70 --comparison-operator GreaterThanThreshold `
      --treat-missing-data notBreaching `
      --alarm-actions $topicArn --region $region
    Write-Host "alarma CPU: $svc"
  }

  # 315 = 70% de ~450 conexiones (db.t4g.medium). Ajustar si cambia la clase (§2.3 del plan).
  aws cloudwatch put-metric-alarm --alarm-name "rds-connections-high" `
    --namespace AWS/RDS --metric-name DatabaseConnections `
    --dimensions Name=DBInstanceIdentifier,Value=$DbInstance `
    --statistic Maximum --period 300 --evaluation-periods 2 `
    --threshold 315 --comparison-operator GreaterThanThreshold `
    --treat-missing-data notBreaching `
    --alarm-actions $topicArn --region $region
  Write-Host "alarma conexiones RDS: $DbInstance"

  # Backlog por cola (nombres reales tomados de los RabbitMQConfig del código)
  $queues = @(
    "solicitudes.jira.queue", "order.payment.result.queue", "payment.order.created.queue",
    "notification.email.queue", "notification.ws.alert.queue", "notification.certificate.queue"
  )
  foreach ($q in $queues) {
    aws cloudwatch put-metric-alarm --alarm-name "mq-backlog-$q" `
      --namespace AWS/AmazonMQ --metric-name MessageReadyCount `
      --dimensions Name=Broker,Value=$BrokerName Name=VirtualHost,Value=/ Name=Queue,Value=$q `
      --statistic Maximum --period 300 --evaluation-periods 1 `
      --threshold 1000 --comparison-operator GreaterThanThreshold `
      --treat-missing-data notBreaching `
      --alarm-actions $topicArn --region $region
    Write-Host "alarma backlog: $q"
  }

  # DLQs: 1 solo mensaje ya es un incidente a revisar
  $dlqs = @(
    "solicitudes.jira.dlq", "order.payment.result.dlq", "payment.order.created.dlq",
    "notification.email.dlq", "notification.ws.alert.dlq", "notification.certificate.dlq"
  )
  foreach ($q in $dlqs) {
    aws cloudwatch put-metric-alarm --alarm-name "mq-dlq-$q" `
      --namespace AWS/AmazonMQ --metric-name MessageCount `
      --dimensions Name=Broker,Value=$BrokerName Name=VirtualHost,Value=/ Name=Queue,Value=$q `
      --statistic Maximum --period 300 --evaluation-periods 1 `
      --threshold 0 --comparison-operator GreaterThanThreshold `
      --treat-missing-data notBreaching `
      --alarm-actions $topicArn --region $region
    Write-Host "alarma DLQ: $q"
  }
}

# ─────────────────────────────────────────────────────────────────────────────
# RDS (Fase 3 §4.2) — COSTO: ~+US$70-85/mes. Requiere instancia 'available'.
# ─────────────────────────────────────────────────────────────────────────────
if ($Rds) {
  Write-Host "== RDS: $DbInstance -> $DbClass Multi-AZ + gp3 =="
  $state = aws rds describe-db-instances --db-instance-identifier $DbInstance `
    --query 'DBInstances[0].DBInstanceStatus' --output text
  if ($state -ne "available") {
    Write-Host "La instancia está '$state'. Arráncala primero:"
    Write-Host "  aws rds start-db-instance --db-instance-identifier $DbInstance"
    exit 1
  }
  aws rds modify-db-instance --db-instance-identifier $DbInstance `
    --db-instance-class $DbClass --multi-az --storage-type gp3 `
    --apply-immediately --region $region `
    --query 'DBInstance.PendingModifiedValues' --output json
  Write-Host "Modificación lanzada (con Multi-AZ el cambio es ~sin downtime)."
  Write-Host "Seguimiento: aws rds describe-db-instances --db-instance-identifier $DbInstance --query 'DBInstances[0].DBInstanceStatus'"
}

# ─────────────────────────────────────────────────────────────────────────────
# Read Replica (Fase 3 §4.2) — COSTO: ~+US$50/mes (otra instancia 24/7).
# Crea una BD de SOLO-LECTURA, copia sincronizada de la principal, para
# descargar las consultas de lectura (reportes/listados) y evitar la sobrecarga.
# Requiere que la instancia principal esté 'available'.
# ─────────────────────────────────────────────────────────────────────────────
if ($ReadReplica) {
  Write-Host "== Read Replica: $ReplicaId (copia de solo-lectura de $DbInstance) =="
  $state = aws rds describe-db-instances --db-instance-identifier $DbInstance `
    --query 'DBInstances[0].DBInstanceStatus' --output text
  if ($state -ne "available") {
    Write-Host "La instancia principal está '$state'. Debe estar 'available'. Arráncala:"
    Write-Host "  aws rds start-db-instance --db-instance-identifier $DbInstance"
    exit 1
  }
  aws rds create-db-read-replica `
    --db-instance-identifier $ReplicaId `
    --source-db-instance-identifier $DbInstance `
    --db-instance-class $DbClass `
    --region $region `
    --query 'DBInstance.{Replica:DBInstanceIdentifier,Estado:DBInstanceStatus}' --output json
  Write-Host "Réplica creándose (tarda ~10-20 min)."
  Write-Host "Las apps de SOLO LECTURA deben apuntar al endpoint de la réplica:"
  Write-Host "  aws rds describe-db-instances --db-instance-identifier $ReplicaId --query 'DBInstances[0].Endpoint.Address' --output text"
  Write-Host "Para borrarla luego (dejar de pagar):"
  Write-Host "  aws rds delete-db-instance --db-instance-identifier $ReplicaId --skip-final-snapshot"
}

# ─────────────────────────────────────────────────────────────────────────────
# Auto Scaling (Fase 3 §4.1) — OJO: con MinTasks>=1 las tareas ARRANCAN y facturan.
# Ejecutar solo con el entorno encendido y DESPUÉS de -Rds (§2.3 del plan).
# ─────────────────────────────────────────────────────────────────────────────
if ($AutoScaling) {
  Write-Host "== Auto Scaling en servicios críticos (min=$MinTasks max=$MaxTasks, target CPU 60%) =="
  Write-Host "ADVERTENCIA: esto pondrá cada servicio crítico en al menos $MinTasks tareas corriendo."
  $policy = '{"TargetValue":60.0,"PredefinedMetricSpecification":{"PredefinedMetricType":"ECSServiceAverageCPUUtilization"},"ScaleOutCooldown":60,"ScaleInCooldown":300}'
  foreach ($svc in $criticalServices) {
    aws application-autoscaling register-scalable-target --service-namespace ecs `
      --scalable-dimension ecs:service:DesiredCount `
      --resource-id "service/$Cluster/$svc" `
      --min-capacity $MinTasks --max-capacity $MaxTasks --region $region | Out-Null
    aws application-autoscaling put-scaling-policy --service-namespace ecs `
      --scalable-dimension ecs:service:DesiredCount `
      --resource-id "service/$Cluster/$svc" `
      --policy-name "cpu-target-60" --policy-type TargetTrackingScaling `
      --target-tracking-scaling-policy-configuration $policy `
      --region $region | Out-Null
    Write-Host "autoscaling ok: $svc"
  }
  Write-Host "Para apagar el entorno luego: des-registrar targets o poner min/max en 0."
}

Write-Host ""
Write-Host "Listo. Verifica el estado con los comandos del Anexo 8 del plan."
