# ========================================
# APAGAR todos los servicios ECS (desired=0)
# ========================================
$cluster = "industrial-safety-cluster"
$region = "us-east-1"

$services = @(
    "user-service-service-do5b3398",
    "chat-service-service-xf2hmld7",
    "exam-service-service-2nw0ujch",
    "industrial-safety-keycloak-task-service-l6iivm6a",
    "payment-service-service-4aky5hec",
    "course-service-service-ifmrw0rc",
    "api-gateway-service-41zhurrw",
    "ngrok-tunnel-service",
    "order-service-service-rymvdlcl",
    "safety-service-service-6r85geni",
    "notification-service-service-jxfxyfhi",
    "purchase-service-service-ulonme67"
)

foreach ($svc in $services) {
    Write-Host "Apagando $svc..."
    aws ecs update-service --cluster $cluster --service $svc --desired-count 0 --region $region --no-cli-pager | Out-Null
}

Write-Host ""
Write-Host "✅ Todos los servicios bajados a 0."
Write-Host "💡 Recuerda pausar RDS desde la consola de AWS."
