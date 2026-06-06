# Guía de configuración — GitHub Actions + AWS

Los archivos de workflow ya están creados:
- `.github/workflows/ci.yml` — pruebas unitarias + integración (se dispara en todo push/PR)
- `.github/workflows/deploy.yml` — build + push ECR + deploy ECS (se dispara solo cuando CI pasa en `main`)

Lo único que falta es configurar AWS y los secretos en GitHub. Son **6 pasos, uno solo se hace en AWS y el resto es copiar/pegar**.

---

## Paso 0 — Obtener tu Account ID de AWS

Necesitas este número en varios pasos:

```bash
aws sts get-caller-identity --query Account --output text
# Ejemplo de salida: 160631388633
```

Guárdalo, lo usarás como `TU_ACCOUNT_ID`.

---

## Paso 1 — Crear el OIDC Provider en IAM (una sola vez en toda la cuenta)

Esto permite que GitHub Actions se autentique en AWS sin credenciales estáticas.

### Opción A — Consola AWS

1. Ve a **IAM → Identity providers → Add provider**
2. Completa así:

| Campo | Valor exacto |
|---|---|
| Provider type | **OpenID Connect** |
| Provider URL | `https://token.actions.githubusercontent.com` |
| Audience | `sts.amazonaws.com` |

3. Clic en **Add provider**

### Opción B — AWS CLI (más rápido)

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

> Si ya existe el provider verás un error `EntityAlreadyExists` — ignóralo, está bien.

---

## Paso 2 — Crear el IAM Role para GitHub Actions

### 2.1 Crear el archivo trust-policy.json

Crea este archivo en cualquier carpeta de tu máquina (luego puedes borrarlo):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::TU_ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:ricardoIsmael/industrial-security-backend:*"
        }
      }
    }
  ]
}
```

> **Reemplaza** `TU_ACCOUNT_ID` con el número del Paso 0.
> El `sub` usa `ricardoIsmael/industrial-security-backend` — si el nombre exacto del repo en GitHub es diferente, corrígelo aquí.

### 2.2 Crear el rol

```bash
aws iam create-role \
  --role-name GitHubActions-IndustrialSafety \
  --assume-role-policy-document file://trust-policy.json
```

### 2.3 Crear el archivo permissions-policy.json

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ECRPush",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECSDeploy",
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices",
        "ecs:DescribeTaskDefinition",
        "ecs:RegisterTaskDefinition"
      ],
      "Resource": "*"
    },
    {
      "Sid": "PassRole",
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": "arn:aws:iam::TU_ACCOUNT_ID:role/ecsTaskExecutionRole"
    }
  ]
}
```

> **Reemplaza** `TU_ACCOUNT_ID`. Si tu rol de ejecución de ECS tiene un nombre diferente a `ecsTaskExecutionRole`, cámbialo.

### 2.4 Adjuntar la política al rol

```bash
aws iam put-role-policy \
  --role-name GitHubActions-IndustrialSafety \
  --policy-name ECS-ECR-Deploy \
  --policy-document file://permissions-policy.json
```

### 2.5 Obtener el ARN del rol (lo necesitas en el Paso 3)

```bash
aws iam get-role \
  --role-name GitHubActions-IndustrialSafety \
  --query 'Role.Arn' \
  --output text
# Salida: arn:aws:iam::160631388633:role/GitHubActions-IndustrialSafety
```

---

## Paso 3 — Configurar los 4 secretos en GitHub

Ve a tu repositorio en GitHub → **Settings → Secrets and variables → Actions → New repository secret**

Crea estos 4 secretos exactamente con estos nombres:

| Secret | Cómo obtener el valor |
|---|---|
| `AWS_ROLE_ARN` | Salida del Paso 2.5 — `arn:aws:iam::160631388633:role/GitHubActions-IndustrialSafety` |
| `AWS_REGION` | La región donde está tu infraestructura — ej. `us-east-1` |
| `ECR_REGISTRY` | `TU_ACCOUNT_ID.dkr.ecr.TU_REGION.amazonaws.com` — ej. `160631388633.dkr.ecr.us-east-1.amazonaws.com` |
| `ECS_CLUSTER` | Nombre exacto de tu cluster ECS. Obtenerlo con: `aws ecs list-clusters --output text` |

> No uses `AWS_ACCESS_KEY_ID` ni `AWS_SECRET_ACCESS_KEY`. Con OIDC no los necesitas.

---

## Paso 4 — Verificar que cada servicio tiene Dockerfile

Cada microservicio necesita un `Dockerfile` en su carpeta raíz para que el deploy funcione.
Comprueba cuáles ya tienen:

```bash
# Desde la raíz del repo
ls */Dockerfile 2>/dev/null
```

Para los que falten, usa esta plantilla mínima de Spring Boot con Java 25:

```dockerfile
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Los 11 servicios que necesitan Dockerfile son:
- `safety-service/Dockerfile`
- `purchase-service/Dockerfile`
- `solicitudes-service/Dockerfile`
- `course-service/Dockerfile`
- `exam-service/Dockerfile`
- `chat-service/Dockerfile`
- `order-service/Dockerfile`
- `payment-service/Dockerfile`
- `user-service/Dockerfile`
- `notification-service/Dockerfile`
- `api-gateway/Dockerfile`

---

## Paso 5 — Verificar los nombres ECS de cada servicio

El `deploy.yml` usa estos nombres para el `--service` de ECS:

| Servicio en el repo | Nombre ECS asumido |
|---|---|
| safety-service | safety-service |
| purchase-service | purchase-service |
| solicitudes-service | solicitudes-service |
| course-service | course-service |
| exam-service | exam-service |
| chat-service | chat-service |
| order-service | order-service |
| payment-service | payment-service |
| user-service | user-service |
| notification-service | notification-service |
| api-gateway | api-gateway |

Verifica que coincidan con los nombres reales en ECS:

```bash
aws ecs list-services --cluster TU_CLUSTER_NAME --output text
```

Si algún nombre es diferente, edita `.github/workflows/deploy.yml` y cambia el valor `ecs:` de la fila correspondiente en la matrix.

---

## Paso 6 — Primer disparo del pipeline

Una vez completados los pasos anteriores:

1. Haz cualquier cambio pequeño (ej. un espacio en un README)
2. Commit y push a `main`:
   ```bash
   git add .
   git commit -m "activar pipeline CI/CD"
   git push origin main
   ```
3. Ve a **GitHub → pestaña Actions**
4. Verás dos workflows corriendo en secuencia:
   - **CI — Industrial Safety Backend** → ejecuta pruebas unitarias e integración
   - **CD — Deploy to AWS ECS** → solo arranca si el CI termina en verde

### Qué esperar en cada job del CI

| Job | Cuánto tarda aprox. | Qué hace |
|---|---|---|
| Unit — \<servicio\> (×10) | 2-4 min c/u en paralelo | `mvn test -Dgroups='!integration'` |
| IT — \<servicio\> (×9) | 3-6 min c/u en paralelo | `mvn failsafe:integration-test` con Testcontainers |
| Cobertura ≥ 80% (×9) | 2-3 min c/u | `mvn verify` con JaCoCo |

### Qué esperar en cada job del CD

| Step | Qué hace |
|---|---|
| Build JAR | `mvn package -DskipTests` |
| AWS OIDC auth | Autentica sin credenciales estáticas |
| ECR login | `docker login` al registry de AWS |
| Docker build + push | Sube imagen con tag `SHA` y `latest` |
| ECS update-service | `--force-new-deployment` |
| ECS wait | Espera hasta que la nueva tarea esté `RUNNING` |

---

## Troubleshooting

| Error en Actions | Causa | Solución |
|---|---|---|
| `Could not assume role` | Trust policy incorrecta | Verifica que el `sub` en la trust policy tenga el nombre exacto del repo de GitHub |
| `ECR: not authorized` | Permisos de ECR faltantes en el rol | Verifica `permissions-policy.json` y que esté adjuntado al rol |
| `Service not found in cluster` | Nombre ECS incorrecto | Ejecuta `aws ecs list-services --cluster TU_CLUSTER` y corrige en `deploy.yml` |
| `./mvnw: Permission denied` | mvnw sin permiso de ejecución en Linux | Ejecuta `git update-index --chmod=+x */mvnw` y haz commit |
| `Image not found` en ECS | Task definition apunta a imagen antigua | Asegúrate que la task definition use `:latest` o actualiza el task def en el pipeline |
| Tests de integración fallan en CI | No debería ocurrir — GitHub Actions tiene Docker nativo | Revisa los logs del step `IT — <servicio>` |

---

## Verificación rápida de que todo está bien configurado

Antes de hacer el primer push, verifica:

```bash
# 1. El OIDC provider existe
aws iam list-open-id-connect-providers

# 2. El rol existe y tiene la política
aws iam get-role --role-name GitHubActions-IndustrialSafety --query 'Role.Arn'
aws iam list-role-policies --role-name GitHubActions-IndustrialSafety

# 3. Los secretos están en GitHub (los ves en Settings → Secrets, no puedes ver el valor pero sí que existen)

# 4. Los Dockerfiles existen
ls safety-service/Dockerfile purchase-service/Dockerfile exam-service/Dockerfile
```
