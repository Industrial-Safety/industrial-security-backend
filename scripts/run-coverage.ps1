# run-coverage.ps1 — Ejecuta tests y abre el reporte de cobertura JaCoCo localmente
# Uso: .\scripts\run-coverage.ps1 -Service course-service
# Uso: .\scripts\run-coverage.ps1 -Service notification-service

param(
    [Parameter(Mandatory = $false)]
    [ValidateSet("course-service", "notification-service", "all")]
    [string]$Service = "all"
)

$services = if ($Service -eq "all") {
    @("course-service", "notification-service")
} else {
    @($Service)
}

foreach ($svc in $services) {
    Write-Host "`n=== Ejecutando pruebas unitarias: $svc ===" -ForegroundColor Cyan

    Push-Location $svc
    try {
        # Pruebas unitarias
        mvn test -Dspring.profiles.active=test -Dgroups="!integration" --no-transfer-progress
        if ($LASTEXITCODE -ne 0) {
            Write-Host "FALLO en pruebas unitarias de $svc" -ForegroundColor Red
            Pop-Location
            exit 1
        }

        # Pruebas de integración (requiere Docker)
        Write-Host "`n=== Ejecutando pruebas de integración: $svc ===" -ForegroundColor Yellow
        mvn failsafe:integration-test failsafe:verify `
            -Dspring.profiles.active=test `
            -Dgroups="integration" `
            --no-transfer-progress

        # Generar reporte JaCoCo
        mvn jacoco:report --no-transfer-progress

        $reportPath = "target\site\jacoco\index.html"
        if (Test-Path $reportPath) {
            Write-Host "`nReporte de cobertura generado: $svc\$reportPath" -ForegroundColor Green
            Start-Process $reportPath
        }
    } finally {
        Pop-Location
    }
}

Write-Host "`n=== Todos los reportes generados correctamente ===" -ForegroundColor Green
