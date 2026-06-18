# ──────────────────────────────────────────────────────────────────────────
# 01 - Crear repositorios ECR en la cuenta NUEVA (560904638369)
#
# Fuente de la verdad: imagenes referenciadas por las task definitions de la
# cuenta vieja -> prefijo "industrial-security/<servicio>".
#
# Uso:
#   pwsh scripts/migracion-aws/01-crear-ecr.ps1
#
# Requisitos: perfil 'nueva' configurado (aws configure --profile nueva).
# Idempotente: si el repo ya existe, lo salta.
# ──────────────────────────────────────────────────────────────────────────
$ErrorActionPreference = "Stop"
$PROFILE_AWS = "nueva"
$REGION      = "us-east-1"

$repos = @(
    "industrial-security/api-gateway",
    "industrial-security/user-service",
    "industrial-security/safety-service",
    "industrial-security/course-service",
    "industrial-security/order-service",
    "industrial-security/payment-service",
    "industrial-security/purchase-service",
    "industrial-security/exam-service",
    "industrial-security/chat-service",
    "industrial-security/notification-service",
    "industrial-security/solicitudes-service"
)

foreach ($repo in $repos) {
    $exists = $true
    try {
        aws ecr describe-repositories --repository-names $repo `
            --profile $PROFILE_AWS --region $REGION 2>$null | Out-Null
        if ($LASTEXITCODE -ne 0) { $exists = $false }
    } catch { $exists = $false }

    if ($exists) {
        Write-Host "[=] Ya existe: $repo"
    } else {
        aws ecr create-repository `
            --repository-name $repo `
            --image-tag-mutability MUTABLE `
            --image-scanning-configuration scanOnPush=true `
            --profile $PROFILE_AWS --region $REGION | Out-Null
        Write-Host "[+] Creado:    $repo"
    }
}

Write-Host ""
Write-Host "=== Repos ECR en la cuenta nueva ==="
aws ecr describe-repositories --profile $PROFILE_AWS --region $REGION `
    --query "sort_by(repositories,&repositoryName)[].repositoryName" --output text
