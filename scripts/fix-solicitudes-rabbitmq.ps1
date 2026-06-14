<#
  fix-solicitudes-rabbitmq.ps1 — Corrige el host de RabbitMQ (stale) en solicitudes-service
  y, opcionalmente, setea el JIRA_API_TOKEN. Registra task-def nueva y redespliega.

  USO:
    ./fix-solicitudes-rabbitmq.ps1
    ./fix-solicitudes-rabbitmq.ps1 -JiraToken "ATATT...tu-token"   # ademas arregla JIRA
#>
param(
  [string]$JiraToken,
  [string]$NewHost = "b-2451d61c-1e40-488a-929f-505d46ca6b57.mq.us-east-1.on.aws",
  [string]$Cluster = "industrial-safety-cluster",
  [string]$Service = "solicitudes-service-service-xs6kjois",
  [string]$Family  = "solicitudes-service"
)
$ErrorActionPreference = "Stop"
$region = "us-east-1"

$td = aws ecs describe-task-definition --task-definition $Family --query "taskDefinition" --output json --region $region | ConvertFrom-Json

# 1) Corregir host de RabbitMQ
($td.containerDefinitions[0].environment | Where-Object { $_.name -eq "SPRING_RABBITMQ_HOST" }).value = $NewHost

# 2) (opcional) Setear JIRA_API_TOKEN
if ($JiraToken) {
  $jiraVar = $td.containerDefinitions[0].environment | Where-Object { $_.name -eq "JIRA_API_TOKEN" }
  if ($jiraVar) { $jiraVar.value = $JiraToken }
  else {
    $td.containerDefinitions[0].environment += [pscustomobject]@{ name = "JIRA_API_TOKEN"; value = $JiraToken }
  }
  Write-Host "JIRA_API_TOKEN configurado."
}

# Quitar campos read-only
"taskDefinitionArn","revision","status","requiresAttributes","compatibilities","registeredAt","registeredBy" |
  ForEach-Object { $td.PSObject.Properties.Remove($_) }

$td | ConvertTo-Json -Depth 30 | Out-File -Encoding ascii "$env:TEMP\soltd.json"
$newArn = aws ecs register-task-definition --cli-input-json "file://$env:TEMP/soltd.json" --query "taskDefinition.taskDefinitionArn" --output text --region $region
Write-Host "Nueva task-def: $newArn"
aws ecs update-service --cluster $Cluster --service $Service --task-definition $newArn --region $region --query "service.serviceName" --output text
Write-Host "Redespliegue lanzado. Verifica en ~2-3 min los logs de solicitudes (no debe haber UnknownHostException)."
