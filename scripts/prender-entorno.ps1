<#
  prender-entorno.ps1 — Enciende el entorno completo (RDS + 13 servicios ECS).
  Pensado para ejecutarse por la Tarea Programada de Windows (encendido 2pm).
  Arranca la RDS, espera a que esté lista, y pone todos los servicios en 1 tarea.
#>
$ErrorActionPreference = "Continue"
$c = "industrial-safety-cluster"
$region = "us-east-1"
$db = "db-industrial-safety"

$svcs = @(
  "api-gateway-service-41zhurrw","user-service-service-do5b3398","safety-service-service-6r85geni",
  "solicitudes-service-service-xs6kjois","order-service-service-rymvdlcl","payment-service-service-4aky5hec",
  "purchase-service-service-ulonme67","course-service-service-ifmrw0rc","exam-service-service-2nw0ujch",
  "chat-service-service-xf2hmld7","notification-service-service-jxfxyfhi",
  "industrial-safety-keycloak-task-service-l6iivm6a","ngrok-tunnel-service"
)

"[$(Get-Date -f HH:mm:ss)] Arrancando RDS $db ..." | Tee-Object -Append "$PSScriptRoot\prender.log"
$state = aws rds describe-db-instances --db-instance-identifier $db --query 'DBInstances[0].DBInstanceStatus' --output text --region $region
if ($state -eq "stopped") {
  aws rds start-db-instance --db-instance-identifier $db --region $region | Out-Null
  aws rds wait db-instance-available --db-instance-identifier $db --region $region
}
"[$(Get-Date -f HH:mm:ss)] RDS lista. Encendiendo servicios ..." | Tee-Object -Append "$PSScriptRoot\prender.log"
foreach ($s in $svcs) {
  aws ecs update-service --cluster $c --service $s --desired-count 1 --region $region --query 'service.serviceName' --output text | Tee-Object -Append "$PSScriptRoot\prender.log"
}
"[$(Get-Date -f HH:mm:ss)] Entorno encendido. (ngrok/keycloak: si keycloak reinicia, actualiza DuckDNS)" | Tee-Object -Append "$PSScriptRoot\prender.log"
