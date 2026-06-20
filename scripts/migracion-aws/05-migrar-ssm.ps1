# ──────────────────────────────────────────────────────────────────────────
# 05 - Migrar parámetros SSM a la cuenta NUEVA (560904638369)
#
# Copia/transforma los 49 parámetros de /config a la cuenta nueva:
#   - Keycloak/issuer-uri/Redis(host,port)/region/buckets/emails -> se copian igual
#   - RabbitMQ host + datasource.url -> se sobreescriben a la infra nueva (+ sslmode=require)
#   - Credenciales (datasource, rabbitmq, keycloak admin) -> valores nuevos conocidos
#   - 3 secretos (redis.url, mercadopago x2) -> se migran en el paso 06 (lectura del viejo)
#
# Uso:  pwsh scripts/migracion-aws/05-migrar-ssm.ps1
# Idempotente: usa --overwrite.
# ──────────────────────────────────────────────────────────────────────────
$ErrorActionPreference = "Stop"
$prof = "nueva"
$secretsFile = Join-Path $PSScriptRoot "secretos-cuenta-nueva.local.txt"

# --- Infra nueva ---
$RDS         = "db-industrial-safety.cavogy2wu2yn.us-east-1.rds.amazonaws.com"
$RABBIT_HOST = "b-a2de3024-89ed-4071-b36c-ae9f76ac58b3.mq.us-east-1.on.aws"
$RDS_PASS    = "Nomeolvidarex3+"
$KC          = "http://industrial-safety.duckdns.org:8080"
$ISSUER      = "$KC/realms/industrial-safety"
$BUCKET      = "industrial-safety-videos-2026"

# Password de RabbitMQ desde el archivo local (bloque [RabbitMQ ...])
$content = Get-Content $secretsFile -Raw
$RABBIT_PASS = [regex]::Match($content, '(?s)\[RabbitMQ.*?Password:\s*(\S+)').Groups[1].Value
if (-not $RABBIT_PASS) { throw "No pude leer la password de RabbitMQ del archivo local" }

function Put($name, $value, $type) {
    aws ssm put-parameter --name $name --value $value --type $type --overwrite --profile $prof --query Version --output text | Out-Null
    if ($LASTEXITCODE -eq 0) { Write-Host "[ok]    $name" } else { Write-Host "[FALLO] $name" }
}

# ── String: se copian igual ───────────────────────────────────────────────
Put "/config/application/cloud.aws.region.static"        "us-east-1" "String"
Put "/config/application/keycloak.auth-server-url"        $KC         "String"
Put "/config/application/spring.data.redis.host"          "genuine-mollusk-124462.upstash.io" "String"
Put "/config/application/spring.data.redis.port"          "6379"      "String"
Put "/config/course-service/app.management.email"         "gerencia@industrialsafety.com" "String"
Put "/config/course-service/aws.region"                   "us-east-1" "String"
Put "/config/course-service/aws.s3.bucket-name"           $BUCKET     "String"
Put "/config/course-service/cloud.aws.s3.bucket-name"     $BUCKET     "String"
Put "/config/exam-service/aws.s3.bucket-name"             $BUCKET     "String"
Put "/config/payment-service/aws.s3.bucket-name"          $BUCKET     "String"
Put "/config/user-service/aws.s3.bucket-name"             $BUCKET     "String"
Put "/config/user-service/cloud.aws.region.static"        "us-east-1" "String"
Put "/config/user-service/keycloak.auth-server-url"       $KC         "String"
Put "/config/user-service/keycloak.realm"                 "industrial-safety" "String"
Put "/config/user-service/keycloak.server-url"            $KC         "String"

# ── String: RabbitMQ (host sobreescrito) ──────────────────────────────────
Put "/config/application/spring.rabbitmq.host"            $RABBIT_HOST "String"
Put "/config/application/spring.rabbitmq.port"            "5671"       "String"
Put "/config/application/spring.rabbitmq.ssl.enabled"     "true"       "String"

# ── String: issuer-uri (DuckDNS, se copia igual) ──────────────────────────
foreach ($svc in @("api-gateway","course-service","exam-service","order-service","payment-service","purchase-service")) {
    Put "/config/$svc/spring.security.oauth2.resourceserver.jwt.issuer-uri" $ISSUER "String"
}

# ── datasource por servicio (url sobreescrita + creds nuevas) ──────────────
$dbmap = [ordered]@{
    "user-service"="user-db"; "safety-service"="epp-db"; "order-service"="order-db";
    "payment-service"="payment-db"; "purchase-service"="purchase-db"; "exam-service"="exam-db"
}
foreach ($svc in $dbmap.Keys) {
    $db = $dbmap[$svc]
    Put "/config/$svc/spring.datasource.url"      "jdbc:postgresql://${RDS}:5432/${db}?sslmode=require&tcpKeepAlive=true" "String"
    Put "/config/$svc/spring.datasource.username" "postgres"  "SecureString"
    Put "/config/$svc/spring.datasource.password" $RDS_PASS   "SecureString"
}

# ── SecureString: credenciales conocidas nuevas ───────────────────────────
Put "/config/application/spring.rabbitmq.username" "industrial"  "SecureString"
Put "/config/application/spring.rabbitmq.password" $RABBIT_PASS  "SecureString"
Put "/config/user-service/keycloak.admin.username" "admin"       "SecureString"
Put "/config/user-service/keycloak.admin.password" "admin"       "SecureString"

Write-Host ""
Write-Host "Parte 1 lista. Faltan 3 secretos a copiar del viejo (paso 06):"
Write-Host "  /config/application/spring.data.redis.url"
Write-Host "  /config/payment-service/mercadopago.api.access-token"
Write-Host "  /config/payment-service/mercadopago.webhook.secret"
