# ──────────────────────────────────────────────────────────────────────────
# 02 - Crear broker Amazon MQ (RabbitMQ) en la cuenta NUEVA (560904638369)
#
# Replica el broker viejo: RabbitMQ 3.13, mq.m7g.medium, SINGLE_INSTANCE.
# Genera una contraseña fuerte y la guarda en secretos-cuenta-nueva.local.txt
# (gitignored). Esas credenciales luego van a SSM (spring.rabbitmq.username/password).
#
# Uso:  pwsh scripts/migracion-aws/02-crear-rabbitmq.ps1
# Idempotente: si el broker ya existe, no lo recrea.
# ──────────────────────────────────────────────────────────────────────────
$ErrorActionPreference = "Stop"
$PROFILE_AWS  = "nueva"
$REGION       = "us-east-1"
$BROKER_NAME  = "industrial-security-rabbitmq"
$ENGINE_VER   = "3.13"
$INSTANCE     = "mq.m7g.medium"
$MQ_USER      = "industrial"
$SECRETS_FILE = Join-Path $PSScriptRoot "secretos-cuenta-nueva.local.txt"

# ¿Ya existe?
$existingId = aws mq list-brokers --profile $PROFILE_AWS --region $REGION `
    --query "BrokerSummaries[?BrokerName=='$BROKER_NAME'].BrokerId" --output text
if ($existingId) {
    Write-Host "[=] El broker '$BROKER_NAME' ya existe (BrokerId: $existingId). No se recrea."
    return
}

# Contraseña fuerte (24 chars alfanumericos -> cumple requisitos de Amazon MQ, sin comas/espacios)
$MQ_PASS = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 24 | ForEach-Object {[char]$_})

$result = aws mq create-broker `
    --broker-name $BROKER_NAME `
    --engine-type RABBITMQ `
    --engine-version $ENGINE_VER `
    --host-instance-type $INSTANCE `
    --deployment-mode SINGLE_INSTANCE `
    --publicly-accessible `
    --auto-minor-version-upgrade `
    --users Username=$MQ_USER,Password=$MQ_PASS `
    --profile $PROFILE_AWS --region $REGION --output json | ConvertFrom-Json

$brokerId = $result.BrokerId

# Guardar credenciales en archivo local gitignored
$stamp = Get-Date -Format "yyyy-MM-dd HH:mm"
Add-Content -Path $SECRETS_FILE -Encoding utf8 -Value @"
[RabbitMQ / Amazon MQ] ($stamp)
  BrokerName: $BROKER_NAME
  BrokerId:   $brokerId
  Username:   $MQ_USER
  Password:   $MQ_PASS
"@

Write-Host "[+] Broker en creacion. BrokerId: $brokerId"
Write-Host "    Credenciales guardadas en: $SECRETS_FILE (gitignored)"
Write-Host "    La provision tarda ~15-25 min. Endpoint AMQPS (5671) disponible al quedar RUNNING."
