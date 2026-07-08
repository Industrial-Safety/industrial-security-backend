# Plan de Recuperación ante Desastres (DRP) — SafeIndustrial

**Proyecto:** Plataforma de Gestión de Seguridad Industrial (SafeIndustrial)  
**Marco de Referencia:** ISO/IEC 27031 (Continuidad de TI) + ISO 24762 (Directrices para Servicios de Recuperación ante Desastres de TI) + ITIL v4 (Gestión de la Continuidad del Servicio de TI)  
**Fecha de Elaboración:** 2026-07-08  
**Versión:** 1.0  
**Clasificación:** Confidencial / Uso Interno  

---

## 1. Introducción

### 1.1 Propósito del documento
El propósito de este Plan de Recuperación ante Desastres (DRP, por sus siglas en inglés *Disaster Recovery Plan*) es establecer, documentar y formalizar las directrices, roles de gobernanza y procedimientos técnicos necesarios para restaurar la infraestructura, bases de datos y microservicios de la plataforma **SafeIndustrial** en caso de un desastre o falla catastrófica del proveedor de nube principal (AWS). El objetivo último es minimizar la pérdida de datos y el tiempo de inactividad operativa para garantizar la seguridad industrial en las plantas cliente.

### 1.2 Alcance
Este plan abarca todos los componentes lógicos del backend de la plataforma SafeIndustrial alojados en la cuenta de AWS activa (iniciada con 6, `665414916692`) en la región de `us-east-1`, que incluyen:
1.  **Capa de datos:** Instancia RDS PostgreSQL (`db-industrial-safety`).
2.  **Capa de orquestación:** Cluster ECS Fargate (`industrial-safety-cluster`) con sus 11 microservicios activos y el servicio de Keycloak.
3.  **Capa de mensajería:** Broker Amazon MQ (RabbitMQ) (`industrial-security-rabbitmq`).
4.  **Capa de configuración:** Parámetros de SSM Parameter Store.
5.  **Capa de acceso y red:** Configuración de Cloud Map, ALB y el túnel de ingreso (`ngrok-tunnel-service`).

*Nota: Queda fuera del alcance la infraestructura física del cliente y los puestos de trabajo de los usuarios finales.*

### 1.3 Definiciones y acrónimos
*   **DRP (Disaster Recovery Plan):** Plan de Recuperación ante Desastres.
*   **BIA (Business Impact Analysis):** Análisis de Impacto al Negocio.
*   **RTO (Recovery Time Objective):** Tiempo máximo tolerado para restablecer el servicio tras un desastre.
*   **RPO (Recovery Point Objective):** Pérdida máxima tolerada de datos medida en tiempo (intervalo desde el último respaldo).
*   **PITR (Point-in-Time Recovery):** Restauración a un segundo específico usando backups continuos de base de datos.
*   **ECS Fargate:** Servicio de contenedores serverless de AWS.
*   **SSM Parameter Store:** Almacén de configuraciones y secretos de AWS.
*   **DLQ (Dead Letter Queue):** Cola de mensajería para gestionar peticiones fallidas persistentes.

### 1.4 Referencias normativas y documentación relacionada
*   **ISO/IEC 24762:** Directrices para servicios de recuperación ante desastres de tecnología de la información y comunicaciones (TIC).
*   **ISO/IEC 27031:** Directrices para la preparación de la tecnología de la información y la comunicación para la continuidad del negocio.
*   **ITIL v4 IT Service Continuity Management:** Práctica de diseño, desarrollo e implementación de la continuidad de servicios de TI.
*   [Plan_Gestion_Incidentes_SafeIndustrial.md](file:///c:/Users/panc1/OneDrive/Documentos/backend-integrador/industrial-security-backend/Plan_Gestion_Incidentes_SafeIndustrial.md)
*   [Plan_Capacidad_Rendimiento_SafeIndustrial.md](file:///c:/Users/panc1/OneDrive/Documentos/backend-integrador/industrial-security-backend/Plan_Capacidad_Rendimiento_SafeIndustrial.md)

---

## 2. Gobernanza y responsabilidades

### 2.1 Estructura de gobernanza
En caso de desastre, la estructura habitual de mando de TI se suspende y se activa el **Comité de Continuidad y Crisis (CCC)**. El CCC opera desde una ubicación alterna o mediante canales de comunicación dedicados y seguros fuera de la red habitual de la empresa.

```
                  ┌──────────────────────────────────┐
                  │   Comité de Continuidad y Crisis  │ (Aprobación y Dirección)
                  └────────────────┬─────────────────┘
                                   │
                                   ▼
                  ┌──────────────────────────────────┐
                  │    Gestor de Recuperación (GR)   │ (Coordinación y Control)
                  └──────┬────────────────────┬──────┘
                         │                    │
                         ▼                    ▼
              ┌────────────────────┐   ┌────────────────────┐
              │   Equipo DevOps    │   │  Administrador BD  │ (Ejecución de Runbooks)
              │ (ECS, SSM, Network)│   │ (RDS PostgreSQL)   │
              └────────────────────┘   └────────────────────┘
```

### 2.2 Roles y responsabilidades
*   **Comité de Continuidad y Crisis (CCC):**
    *   Integrantes: Líder de Tecnología (CTO), Director de Operaciones y Representante Legal.
    *   Responsabilidad: Evaluar la gravedad de la situación, declarar formalmente el desastre y autorizar los recursos financieros extraordinarios necesarios para la contingencia.
*   **Gestor de Recuperación (GR):**
    *   Responsabilidad: Liderar y coordinar a los equipos técnicos en la ejecución del DRP. Monitorear el progreso frente a los RTO y RPO definidos y reportar de manera constante al CCC.
*   **Equipo DevOps & SysAdmin:**
    *   Responsabilidad: Aprovisionar el cluster ECS Fargate de destino, restaurar la configuración SSM, configurar la red DNS/Cloud Map y redirigir el tráfico.
*   **Administrador de Base de Datos (DBA):**
    *   Responsabilidad: Restaurar la base de datos relacional RDS desde los snapshots autorizados y activar el Point-in-Time Recovery.
*   **Proveedores externos (AWS, DuckDNS, ngrok):**
    *   Responsabilidad: Proveer soporte de Nivel 3 en caso de fallas de plataforma.

### 2.3 Autoridad y aprobación del plan
Este plan es aprobado por el CCC y el Líder de Tecnología de SafeIndustrial. Únicamente el CCC tiene la autoridad para declarar el estado de "Desastre" basándose en los criterios técnicos del apartado 7.1.

---

## 3. Análisis y evaluación

### 3.1 Análisis de Impacto al Negocio (BIA) — Resultados principales
La interrupción de la plataforma SafeIndustrial impacta directamente en la capacidad de las empresas clientes de reportar actos inseguros, programar inspecciones e impartir cursos de seguridad industrial. El BIA determinó que:
*   Una caída mayor a **4 horas** puede provocar que los trabajadores operen en condiciones de riesgo no reportadas, aumentando la tasa de accidentes laborales.
*   El costo reputacional e infracciones a normativas de seguridad laboral (ej. SUNAFIL u organismos homólogos) generan pérdidas financieras significativas de manera exponencial tras las primeras 2 horas.

### 3.2 Evaluación de riesgos y amenazas

| Riesgo / Amenaza | Probabilidad | Impacto | Mitigación Técnica |
|---|---|---|---|
| **Pérdida/Vaciado de Cuenta AWS por Créditos/Billing** | Alta (ocurrió el 2026-07-05) | Crítico | Alertas de presupuesto + backups externos (Regla 3-2-1) + scripts portables fuera de AWS |
| **Fallo en RDS PostgreSQL único compartido** | Media | Alto | Snapshots automáticos diarios + manuales + Point-in-Time Recovery (PITR) de 35 días |
| **Caída del túnel ngrok (punto único de entrada)** | Alta | Alto | Disponer de IP pública estática en ALB y redundancia de túneles alternativos |
| **Ransomware / Inyección maliciosa en base de datos** | Baja | Crítico | Roles AWS IAM específicos y snapshots de RDS cifrados con KMS cross-account |
| **Error humano en despliegue (CI/CD corrupto)** | Alta | Medio | Deployment Circuit Breaker en ECS para rollback automático automático |

### 3.3 Prioridad de servicios y procesos críticos

| Nivel de Prioridad | Servicios / Componentes | Impacto de Caída | RTO Máximo |
|---|---|---|---|
| **Nivel 1 (Crítico)** | `api-gateway`, `user-service`, `safety-service`, `keycloak`, `RDS PG` | Imposibilidad absoluta de login, autenticación y registro de inspecciones/incidentes. | **2 horas** |
| **Nivel 2 (Medio)** | `solicitudes-service`, `order-service`, `payment-service`, `Amazon MQ` | Afecta flujos de compras de cursos e integración de mensajería (las colas acumulan el backlog en DLQs). | **4 horas** |
| **Nivel 3 (Bajo)** | `chat-service`, `course-service`, `exam-service`, `notification-service` | Inasistencia a chats internos, visualización de material educativo y reportes históricos. | **8 horas** |

---

## 4. Objetivos de recuperación

### 4.1 Objetivos de Tiempo de Recuperación (RTO)
*   **Servicios Críticos (Nivel 1):** RTO = **2 horas**. La plataforma base debe estar lista para autenticar usuarios y registrar incidentes en este tiempo.
*   **Servicios Secundarios (Nivel 2 y 3):** RTO = **4 a 8 horas**. Los flujos complementarios pueden tardar más en recuperar su normalidad operativa.

### 4.2 Objetivos de Punto de Recuperación (RPO)
*   **Capa de datos transaccionales (RDS):** 
    *   *Estado Actual:* **24 horas** (respaldos automáticos diarios a las 07:00 UTC).
    *   *Estado Objetivo:* **15 minutos** mediante la habilitación de copias de seguridad continuas y PITR (Point-in-Time Recovery) en RDS.
*   **Capa de microservicios e infraestructura:** RPO = **0**. Son componentes sin estado (*stateless*); su código se encuentra versionado y compilado en imágenes de AWS ECR listas para ser desplegadas.

---

## 5. Inventario y dependencias

### 5.1 Inventario de activos y recursos críticos

| Componente Técnico | Identificador / Recurso AWS | Tamaño / Tipo | Dependencia |
|---|---|---|---|
| **Base de Datos Única** | `db-industrial-safety` | db.t4g.micro (PostgreSQL 16) | Almacenamiento gp2 20GB |
| **Servicio de Autenticación** | `industrial-safety-keycloak-task-service` | ECS Task (0.5 vCPU / 2 GB) | `keycloak-db` (en RDS PG) |
| **Orquestador Backend** | `industrial-safety-cluster` | AWS ECS Fargate Cluster | 11 Microservicios activos |
| **Cola de Mensajería** | `industrial-security-rabbitmq` | mq.m7g.medium (Single Instance) | Amazon MQ |
| **Almacén de Configuración** | AWS Parameter Store | us-east-1 Región | SSM Client |

### 5.2 Dependencias internas y externas

```
   ┌───────────────┐        ┌───────────────┐
   │    GitHub     ├───────►│  GitHub App   │ (Pipeline CI/CD y Código Fuente)
   └───────────────┘        └───────┬───────┘
                                    │
                                    ▼
   ┌───────────────┐        ┌───────────────┐        ┌───────────────┐
   │    DuckDNS    ├───────►│  ngrok Tunnel ├───────►│  api-gateway  │ (Ingreso de Tráfico)
   └───────────────┘        └───────────────┘        └───────────────┘
```

*   **GitHub / GitHub Actions:** Proveedor del código fuente y encargado del pipeline de despliegue para regenerar contenedores en ECR.
*   **DuckDNS & ngrok:** Resuelven el acceso de red externo del usuario al entorno de AWS sin necesidad de un túnel VPN directo o balanceadores costosos en fase de desarrollo.

### 5.3 Contratos y acuerdos de nivel de servicio (SLA/OLA)
*   **SLA AWS Fargate:** 99.9% de disponibilidad mensual.
*   **SLA AWS RDS Single-AZ:** 99.9% de disponibilidad mensual (Nota: Para producción se recomienda Multi-AZ con 99.95%).
*   **OLA Interno (Equipo DevOps):** Respuesta y asignación técnica en un máximo de **15 minutos** tras el estado de ALARMA en CloudWatch.

---

## 6. Estrategias de recuperación

### 6.1 Estrategias generales
SafeIndustrial adopta la estrategia de **Cold Start / Warm Standby en la misma cuenta utilizando múltiples regiones**:
1.  **Respaldo Automatizado con AWS Backup (Cross-Region):** Se utiliza el servicio de **AWS Backup** centralizado en la cuenta activa (`665414916692`) para automatizar la creación de respaldos. Las copias de seguridad de RDS y S3 se replican automáticamente hacia una **segunda región geográfica (ej. us-west-2)** dentro de la misma cuenta, asegurando la disponibilidad física de los datos sin incurrir en costos de una cuenta secundaria (cumpliendo la regla 3-2-1 de copia fuera del sitio).
2.  **Infraestructura como Código (IaC):** La configuración completa del cluster ECS y Parameter Store se mantiene en scripts de PowerShell (`scripts/apply-capacity-plan.ps1`) y plantillas CloudFormation/Terraform para provisionarse desde cero en una cuenta limpia.
3.  **Sitio de Contingencia Local:** En caso de caída total de AWS en la región `us-east-1`, la estrategia de contingencia de emergencia consiste en el despliegue local o en servidor alterno utilizando el archivo `docker-compose.yml` preconfigurado en el repositorio.

### 6.2 Estrategias por servicio/aplicación
*   **Microservicios Stateless:** Despliegue inmediato desde imágenes almacenadas en ECR en la cuenta de respaldo o reconstrucción desde el repositorio GitHub.
*   **RDS PostgreSQL:** Restauración automatizada a partir de los puntos de recuperación gestionados por **AWS Backup** en la región de contingencia (`us-west-2`), utilizando respaldos continuos para Point-in-Time Recovery (PITR) de hasta 35 días.
*   **Parameter Store (SSM):** Importación masiva del archivo JSON versionado que contiene las llaves y credenciales del backend, respaldado externamente en un bucket de S3 protegido por AWS Backup.

### 6.3 Selección y justificación de opciones de recuperación
Se selecciona esta estrategia porque optimiza los costos operativos y simplifica la gestión mediante el uso exclusivo de **AWS Backup**. Al ser una base de datos pequeña (20 GB base), el RTO de 2 horas es perfectamente alcanzable mediante la restauración de respaldos automatizados sin necesidad de pagar una base de datos replicada 24/7 en caliente (*Active-Active*), lo que incrementaría los costos mensuales en más de un 100%. La automatización mediante AWS Backup hacia la región de contingencia (`us-west-2`) elimina la necesidad de tareas manuales propensas a errores durante un desastre.

---

## 7. Planes y procedimientos de recuperación

### 7.1 Activación del plan — Criterios y proceso de declaración de desastre
La activación del DRP se realizará de acuerdo al siguiente flujo de decisión:

```
        ┌────────────────────────────────────────────────────────┐
        │  ¿Alarma CloudWatch activa o reporte de caída total?    │
        └──────────────────────────┬─────────────────────────────┘
                                   │
                                   ▼
        ┌────────────────────────────────────────────────────────┐
        │  DevOps diagnostica falla de AWS o pérdida de cuenta   │
        └──────────────────────────┬─────────────────────────────┘
                                   │
                                   ▼
        ┌────────────────────────────────────────────────────────┐
        │    ¿El tiempo estimado de resolución supera los 30 min? │
        └──────────┬───────────────────────────────┬─────────────┘
                   │ Sí                            │ No
                   ▼                               ▼
  ┌─────────────────────────────────┐    ┌──────────────────────────────────┐
  │  El CCC declara el DESASTRE     │    │ Se maneja como Incidente Estándar│
  │     (Se inicia el DRP)          │    └──────────────────────────────────┘
  └─────────────────────────────────┘
```

### 7.2 Procedimientos de restauración de datos
Este procedimiento se automatiza mediante **AWS Backup** en la cuenta activa (`665414916692`), con replicación desde la región principal (`us-east-1`) a la región de contingencia (`us-west-2`):

#### Paso 1 — Replicación automática (Cross-Region Copy)
AWS Backup realiza la copia automática de la base de datos RDS y evidencias de S3 al almacén de copias de seguridad (*Backup Vault*) de la región de destino (`us-west-2`).

#### Paso 2 — Restaurar la base de datos (en la región de contingencia)
En caso de desastre regional, el DBA inicia la restauración desde la consola de AWS Backup seleccionando la región de destino (`us-west-2`) mediante la consola o la CLI de AWS:
```bash
# Iniciar la restauración de RDS PostgreSQL desde el punto de recuperación de AWS Backup en la región de contingencia
aws backup start-restore-job \
  --region us-west-2 \
  --recovery-point-arn arn:aws:backup:us-west-2:665414916692:recovery-point:<IDENTIFICADOR_RECOVERY_POINT> \
  --metadata "{\"dbInstanceIdentifier\":\"db-industrial-safety\",\"targetDbInstanceClass\":\"db.t4g.micro\",\"availabilityZone\":\"us-west-2a\"}" \
  --iam-role-arn arn:aws:iam::665414916692:role/service-role/AWSBackupDefaultServiceRole
```

### 7.3 Restauración de infraestructura (red, servidores, almacenamiento)

#### Paso 1 — Cargar los Parámetros en SSM Parameter Store
Restaure la configuración del backend importando el archivo JSON de configuración previa (ver ejemplo en el repositorio):
```powershell
# Ejemplo de script de restauración de SSM
$parameters = Get-Content -Raw -Path "./backups/ssm-parameters-export.json" | ConvertFrom-Json
foreach ($p in $parameters) {
    Write-Host "Restaurando parámetro: $($p.Name)"
    Write-ParameterStore -Name $p.Name -Value $p.Value -Type "String" -Overwrite $true
}
```

#### Paso 2 — Desplegar el Cluster ECS Fargate
Ejecute el script de capacidad del proyecto para asegurar que el cluster y sus servicios asociados se levanten correctamente en AWS con los límites apropiados:
```powershell
./scripts/apply-capacity-plan.ps1 -ClusterName "industrial-safety-cluster" -CircuitBreaker $true
```

#### Paso 3 — Reconfigurar el túnel ngrok y DuckDNS
Actualice la dirección IP pública del nuevo Application Load Balancer (ALB) de AWS en el servicio de DuckDNS para apuntar el dominio `industrial-safety.duckdns.org`.
Reinicie el servicio de túnel local o el contenedor de ECS de ngrok:
```bash
aws ecs update-service --cluster industrial-safety-cluster --service ngrok-tunnel-service --force-new-deployment
```

### 7.4 Procedimientos de recuperación de instalaciones y puesto de trabajo
Si las oficinas principales del equipo DevOps quedan inutilizadas, el personal técnico utilizará la VPN interna y plataformas de chat alternativas (Discord/Slack fuera de la infraestructura corporativa) para operar los despliegues de forma remota.

---

## 8. Gestión de la comunicación

### 8.1 Plan de comunicación interna
*   **Durante la primera hora:** El Gestor de Recuperación enviar una notificación por correo y mensaje de chat al CCC con el diagnóstico inicial.
*   **Actualizaciones periódicas:** Se realizarán briefings técnicos rápidos de **15 minutos** cada hora para actualizar el estado del despliegue y la restauración de datos.

### 8.2 Comunicación externa
*   **Clientes corporativos:** SafeIndustrial notificará a los administradores de seguridad de las empresas cliente mediante un correo electrónico general a través de un servicio de mailing externo (SendGrid/Mailchimp independiente de la infraestructura de producción) indicando la ventana de mantenimiento de emergencia.
*   **Página de Estado:** Mantener una web estática simple de status en GitHub Pages (`status.safeindustrial.github.io`) para informar el progreso público de la restauración.

### 8.3 Contactos críticos y medios alternativos
Se mantiene un canal de comunicación de contingencia en una aplicación externa (por ejemplo, Signal/Telegram) para uso exclusivo de soporte técnico si los correos corporativos basados en la nube primaria fallan.

---

## 9. Gestión de proveedores y servicios externos

### 9.1 Identificación de proveedores críticos
*   **Amazon Web Services (AWS):** Soporte técnico empresarial contratado.
*   **DuckDNS:** Proveedor gratuito de DNS dinámico.
*   **ngrok:** Proveedor de redirección y túneles de desarrollo seguro.

### 9.2 Planes de contingencia con proveedores
*   En caso de indisponibilidad de **DuckDNS**, se utilizará un subdominio alternativo configurado en AWS Route 53 (`contingencia.safeindustrial.com`).
*   En caso de problemas de red en **ngrok**, se habilitará el redireccionamiento directo mediante la IP pública del Gateway y puertos en el firewall.

---

## 10. Pruebas, ejercicios y validación

### 10.1 Programa de pruebas y calendario
Las pruebas del DRP se realizarán de manera obligatoria de forma **semestral**.
*   **Prueba de Invierno (Enero):** Simulacro conceptual y restauración de RDS en cuenta Sandbox.
*   **Prueba de Verano (Julio):** Simulación completa de desastre con apagado total de infraestructura productiva y restauración.

### 10.2 Tipos de pruebas
*   **Prueba de Mesa (Tabletop):** Reunión de los líderes técnicos para recorrer paso a paso el plan y validar que los contactos y asignaciones estén actualizados.
*   **Prueba Técnica Parcial:** Ejecutar la restauración de una copia de base de datos PostgreSQL de producción en una base de datos local Docker para validar consistencia de esquemas.
*   **Prueba Completa (Simulación):** Clonar la infraestructura completa a una cuenta AWS secundaria y simular la desconexión total del sistema principal.

### 10.3 Registro de pruebas y lecciones aprendidas (Incidente del 2026-07-05)
*   **Descripción del Evento Real:** Pérdida de la cuenta AWS activa del proyecto debido al agotamiento súbito de créditos de desarrollo. El backend completo se desactivó de inmediato.
*   **Respuesta Ejecutada:** Se realizó un snapshot manual de RDS (`db-industrial-safety-snapshot` con un peso de 20 GB) antes de que la cuenta fuera vaciada. Se procedió con el teardown total facturable y se importaron 67 parámetros SSM a un archivo JSON.
*   **Lección Aprendida:** Los backups manuales iniciales se almacenaron en la misma cuenta de AWS que estaba en riesgo. **Se violó la regla de respaldo 3-2-1**.
*   **Acción Correctiva:** Centralizar y automatizar los respaldos mediante **AWS Backup**, configurando planes de copias continuas (PITR) y snapshots diarios con replicación automática **Cross-Region (a us-west-2)** dentro de la misma cuenta activa (`665414916692`), garantizando el cumplimiento de la regla 3-2-1.

### 10.4 Mejora continua basada en resultados de pruebas
Cada prueba del DRP generará una bitácora de incidencias. Los scripts de PowerShell para el aprovisionamiento de ECS deben actualizarse si se detecta que los nombres de los servicios o las dependencias de red de Cloud Map han cambiado en el repositorio principal.

---

## 11. Mantenimiento del Plan

### 11.1 Revisión, actualización y control de versiones
Este documento debe actualizarse tras cada cambio mayor de arquitectura del backend (por ejemplo, la adición de nuevos microservicios o cambios de versión de Java/Spring Boot).

### 11.2 Frecuencia de revisiones y responsables
*   **Frecuencia:** Anual o tras un desastre real.
*   **Responsable:** Gestor de Recuperación (DevOps Lead).

### 11.3 Formación y concienciación del personal
Todo desarrollador que se incorpore al equipo de SafeIndustrial debe leer el DRP y participar en al menos un simulacro técnico de restauración local usando Docker Compose durante su inducción.

---

## 12. Anexos

### Anexo A. Matrices de RTO / RPO detalladas

| Microservicio / Recurso | RTO Objetivo | RPO Objetivo | Método de Respaldos |
|---|---|---|---|
| **api-gateway** | 30 min | 0 | Stateless (GitHub Container Registry) |
| **user-service** | 1 hora | 15 min | AWS Backup (Continuo + PITR) |
| **safety-service** | 1 hora | 15 min | AWS Backup (Continuo + PITR) |
| **keycloak** | 1 hora | 1 hora | Exportación periódica del Realm |
| **Amazon MQ** | 2 horas | 0 | Colas persistentes con colas DLQ configuradas |

### Anexo B. Listado de contactos críticos (Simulado para resguardo de datos)
*   **Comité de Crisis (Líder):** +51 999 888 777 (Líder TI)
*   **Coordinador de DRP / DevOps Lead:** +51 999 888 666
*   **Soporte AWS Business:** A través de la consola AWS Support Center (ID de Cuenta activa `665414916692`)

### Anexo C. Inventario básico de SSM Parameter Store a respaldar
El archivo backup local (`backups/ssm-parameters-export.json`) debe incluir como mínimo las variables del proyecto:
*   `SPRING_DATASOURCE_URL`
*   `SPRING_DATASOURCE_USERNAME`
*   `SPRING_DATASOURCE_PASSWORD`
*   `RABBITMQ_HOST`
*   `KEYCLOAK_AUTH_SERVER_URL`

### Anexo E. Formulario de activación del DRP

```
FECHA Y HORA DE DETECCIÓN: _____/_____/_____, _____:_____  
CAUSA RAÍZ IDENTIFICADA: [ ] Caída física AWS  [ ] Pérdida de Cuenta  [ ] Ransomware  [ ] Otro  
FIRMA AUTORIZADA DEL COMITÉ: __________________________________
LUGAR O CUENTA AWS DE RESTAURACIÓN: ____________________________
```

### Anexo H. Diagrama de arquitectura de Alta Disponibilidad (HA)
A continuación se presenta el diseño lógico de alta disponibilidad implementado para el backend de SafeIndustrial en AWS, distribuyendo los servicios en múltiples Zonas de Disponibilidad (Multi-AZ), con base de datos RDS replicada, broker de mensajería redundante, autoescalado de tareas (hasta 4 tareas por servicio), descubrimiento de servicios (Cloud Map), gestión de configuración (Parameter Store), observabilidad (CloudWatch) y replicación de respaldos entre regiones gestionada por AWS Backup:

![Diagrama de Arquitectura Completo de SafeIndustrial](C:/Users/panc1/.gemini/antigravity/brain/98f22959-10f8-4342-ae92-52f666b5197b/safeindustrial_aws_complete_ecosystem_1783526942587.png)

---

## CONCLUSIONES
La implementación formal de este DRP garantiza que la plataforma SafeIndustrial pueda mitigar incidentes catastróficos reduciendo el RTO general por debajo del umbral crítico de 2 horas. La lección aprendida del incidente del 2026-07-05 enfatiza que la automatización de la infraestructura como código (IaC) y el cumplimiento estricto de la regla de respaldos 3-2-1 no son opcionales, sino la base indispensable para sostener la fiabilidad en la gestión de la seguridad industrial.

---

## BIBLIOGRAFÍA
1.  **ISO/IEC 24762:2008** - *Information technology — Security techniques — Guidelines for information and communications technology disaster recovery services*.
2.  **ITIL v4 Foundation** - *Service Financial Management & IT Service Continuity Management*.
3.  **AWS Well-Architected Framework** - *Reliability Pillar: Recovery planning (DR)*.
