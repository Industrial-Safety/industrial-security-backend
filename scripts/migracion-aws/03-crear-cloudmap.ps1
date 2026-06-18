# ──────────────────────────────────────────────────────────────────────────
# 03 - Crear namespace privado de Amazon Cloud Map en la cuenta NUEVA
#
# Replica el namespace viejo: privado (DNS) 'industrial-security' en la VPC default.
# Los servicios individuales (user-service.industrial-security, etc.) se registran
# despues, al crear cada servicio ECS (con TTL 10s).
#
# Uso:  pwsh scripts/migracion-aws/03-crear-cloudmap.ps1
# Idempotente: si el namespace ya existe, no lo recrea.
# ──────────────────────────────────────────────────────────────────────────
$ErrorActionPreference = "Stop"
$PROFILE_AWS = "nueva"
$REGION      = "us-east-1"
$NS_NAME     = "industrial-security"

# VPC default
$VPC_ID = aws ec2 describe-vpcs --profile $PROFILE_AWS --region $REGION `
    --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text

# ¿Ya existe?
$existing = aws servicediscovery list-namespaces --profile $PROFILE_AWS --region $REGION `
    --query "Namespaces[?Name=='$NS_NAME'].Id" --output text
if ($existing) {
    Write-Host "[=] El namespace '$NS_NAME' ya existe (Id: $existing). No se recrea."
    return
}

Write-Host "[*] Creando namespace privado '$NS_NAME' en VPC $VPC_ID ..."
$opId = aws servicediscovery create-private-dns-namespace `
    --name $NS_NAME `
    --vpc $VPC_ID `
    --profile $PROFILE_AWS --region $REGION `
    --query "OperationId" --output text

# Esperar a que la operacion termine
do {
    Start-Sleep -Seconds 5
    $status = aws servicediscovery get-operation --operation-id $opId `
        --profile $PROFILE_AWS --region $REGION --query "Operation.Status" --output text
    Write-Host "    estado: $status"
} while ($status -eq "PENDING" -or $status -eq "SUBMITTED")

if ($status -eq "SUCCESS") {
    $nsId = aws servicediscovery list-namespaces --profile $PROFILE_AWS --region $REGION `
        --query "Namespaces[?Name=='$NS_NAME'].Id" --output text
    Write-Host "[+] Namespace creado. Id: $nsId"
} else {
    Write-Host "[!] La operacion termino en estado: $status"
    aws servicediscovery get-operation --operation-id $opId --profile $PROFILE_AWS --region $REGION --output json
}
