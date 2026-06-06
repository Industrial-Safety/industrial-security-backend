# ========================================
# ENCENDER todos los servicios ECS (desired=1)
# ========================================
$cluster = "industrial-safety-cluster"
$region = "us-east-1"

$services = @(
    "industrial-safety-keycloak-task-service-l6iivm6a",  # Keycloak primero
    "api-gateway-service-41zhurrw",
    "user-service-service-do5b3398",
    "course-service-service-ifmrw0rc",
    "order-service-service-rymvdlcl",
    "payment-service-service-4aky5hec",
    "exam-service-service-2nw0ujch",
    "chat-service-service-xf2hmld7",
    "notification-service-service-jxfxyfhi",
    "purchase-service-service-ulonme67",
    "safety-service-service-6r85geni",
    "ngrok-tunnel-service"                               # ngrok al final
)

foreach ($svc in $services) {
    Write-Host "Encendiendo $svc..."
    aws ecs update-service --cluster $cluster --service $svc --desired-count 1 --region $region --no-cli-pager | Out-Null
}

Write-Host ""
Write-Host "✅ Todos los servicios subidos a 1."
Write-Host "⏳ Espera ~3 minutos para que todos levanten."
Write-Host "💡 Asegúrate de que RDS esté iniciado antes de usar la app."
