<#
.SYNOPSIS
  Reimporta los parametros SSM desde el backup JSON (formato get-parameters-by-path)
  hacia la cuenta AWS destino. Parte del runbook de migracion (KB: "Restaurar BD y
  configuracion en una cuenta nueva").

.EXAMPLE
  .\import-ssm.ps1 -BackupJson "C:\Users\panc1\aws-ssm-backup\ssm-backup-560904638369-20260705.json" -ProfileName cuenta-nueva
#>
param(
    [Parameter(Mandatory = $true)][string]$BackupJson,
    [string]$ProfileName = "default",
    [string]$Region = "us-east-1",
    # Simulacion: muestra que se importaria sin escribir nada.
    [switch]$DryRun
)

if (-not (Test-Path $BackupJson)) {
    Write-Error "No existe el backup: $BackupJson"
    exit 1
}

$data = Get-Content $BackupJson -Raw -Encoding UTF8 | ConvertFrom-Json
$total = $data.Parameters.Count
Write-Host "Parametros en el backup: $total (perfil destino: $ProfileName, region: $Region)"

$ok = 0; $fail = 0
foreach ($p in $data.Parameters) {
    if ($DryRun) {
        Write-Host "[dry-run] $($p.Name) ($($p.Type))"
        continue
    }
    # cli-input-json evita problemas de quoting con valores que traen espacios/simbolos.
    $payload = @{ Name = $p.Name; Value = $p.Value; Type = $p.Type; Overwrite = $true } | ConvertTo-Json -Compress
    $tmp = New-TemporaryFile
    # UTF-8 SIN BOM: el AWS CLI rechaza el JSON si lleva BOM (PS 5.1 lo agrega con Set-Content).
    [System.IO.File]::WriteAllText($tmp.FullName, $payload, (New-Object System.Text.UTF8Encoding($false)))
    aws ssm put-parameter --cli-input-json "file://$tmp" --profile $ProfileName --region $Region --output text | Out-Null
    if ($LASTEXITCODE -eq 0) { $ok++; Write-Host "OK    $($p.Name)" }
    else { $fail++; Write-Warning "FALLO $($p.Name)" }
    Remove-Item $tmp -Force
}

Write-Host ""
Write-Host "Importados: $ok / $total  (fallidos: $fail)"
Write-Host "RECUERDA: actualizar los parametros que cambian con la cuenta (ver README.md):"
Write-Host "  - URLs de la BD (endpoint RDS nuevo)"
Write-Host "  - Host de RabbitMQ (broker MQ nuevo)"
Write-Host "  - Y crear los parametros de eventos-service y conocimiento-service"
