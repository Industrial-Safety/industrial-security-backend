# Migración a la cuenta AWS nueva

Runbook operativo para restaurar SafeIndustrial en otra cuenta (el mismo procedimiento
vive en la Base de Conocimiento de la plataforma, artículo "Restaurar BD y configuración
en una cuenta nueva"). El origen es la cuenta **560904638369** (vaciada el 2026-07-05
por falta de créditos; solo conserva los snapshots RDS y los parámetros SSM).

## Insumos que ya existen

| Insumo | Dónde |
|---|---|
| Snapshot manual de la BD (20 GB) | `db-industrial-safety-snapshot` en la cuenta 560904638369 |
| Backup de los 67 parámetros SSM | `C:\Users\panc1\aws-ssm-backup\ssm-backup-560904638369-20260705.json` |
| Imágenes Docker | Reconstruibles desde este repo por el CD (`deploy.yml`) |
| Templates de task-definition | `deploy/task-definitions/*.json` (los 15 servicios) |

## Pasos

### 1. Compartir el snapshot con la cuenta nueva (¡PRIMERO — la cuenta origen está en riesgo!)

```bash
# En la cuenta VIEJA (perfil 'nueva' = 560904638369):
aws rds modify-db-snapshot-attribute \
  --db-snapshot-identifier db-industrial-safety-snapshot \
  --attribute-name restore --values-to-add <ID_CUENTA_NUEVA> \
  --profile nueva

# En la cuenta NUEVA: copiarlo (queda propio aunque cierren la cuenta vieja)
aws rds copy-db-snapshot \
  --source-db-snapshot-identifier arn:aws:rds:us-east-1:560904638369:snapshot:db-industrial-safety-snapshot \
  --target-db-snapshot-identifier db-industrial-safety-snapshot
```

### 2. Restaurar la base de datos

```bash
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier db-industrial-safety \
  --db-snapshot-identifier db-industrial-safety-snapshot \
  --db-instance-class db.t4g.micro --no-multi-az
```

Luego crear las bases de los servicios nuevos (no vienen en el snapshot):

```sql
CREATE DATABASE "eventos-db";
CREATE DATABASE "conocimiento-db";
```

### 3. Reimportar la configuración SSM

```powershell
.\import-ssm.ps1 -BackupJson "C:\Users\panc1\aws-ssm-backup\ssm-backup-560904638369-20260705.json" -ProfileName <perfil-nuevo>
```

Después **actualizar** los parámetros que cambian con la cuenta:
- `/config/*/spring.datasource.url` → endpoint del RDS nuevo
- `/config/application/spring.rabbitmq.host` → broker MQ nuevo
- `/config/application/keycloak.auth-server-url` e issuer-uri → Keycloak nuevo

Y **crear** los de los servicios nuevos:

```
/config/eventos-service/spring.datasource.url        = jdbc:postgresql://<RDS>:5432/eventos-db?sslmode=require
/config/eventos-service/spring.datasource.username
/config/eventos-service/spring.datasource.password
/config/conocimiento-service/spring.datasource.url   = jdbc:postgresql://<RDS>:5432/conocimiento-db?sslmode=require
/config/conocimiento-service/spring.datasource.username
/config/conocimiento-service/spring.datasource.password
```

### 4. Recrear la infraestructura base

- Roles IAM: `ecsTaskExecutionRole` (+ política de lectura SSM), rol OIDC para GitHub Actions.
- **Actualizar el ID de cuenta en `deploy/task-definitions/*.json`** (buscar y reemplazar `560904638369`).
- Cluster ECS `industrial-safety-cluster`, namespace Cloud Map `industrial-security`.
- Repos ECR (los crea el push del CD si existe permiso, o crearlos: 15 servicios).
- Amazon MQ (RabbitMQ) y Keycloak (recordar el realm role `SOPORTE`).
- Secrets de GitHub Actions: `AWS_REGION`, `ECR_REGISTRY`, `ECS_CLUSTER`, `AWS_ROLE_ARN`.

### 5. Desplegar

Correr el workflow **CD — Deploy to AWS ECS** (workflow_dispatch). La matrix ya incluye
`eventos-service` y `conocimiento-service`. Primera vez: registra las task-definitions
desde los templates; luego crear los servicios ECS apuntando a ellas.

### 6. Validar (checklist del runbook)

- [ ] `actuator/health` UP en todos los servicios
- [ ] Login por Keycloak para cada rol (incluido SOPORTE)
- [ ] Datos históricos presentes (usuarios, cursos, incidencias)
- [ ] Flujo completo: evento (POST /demo) → incidencia → contador SLA → resolución
- [ ] Base de Conocimiento precargada (7 artículos al primer arranque)
- [ ] Registrar los tiempos reales de esta migración (validan el RTO del plan) y
      documentarla en la KB como prueba de restauración
