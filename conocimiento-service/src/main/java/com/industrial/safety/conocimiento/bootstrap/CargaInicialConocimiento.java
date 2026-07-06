package com.industrial.safety.conocimiento.bootstrap;

import com.industrial.safety.conocimiento.entity.Articulo;
import com.industrial.safety.conocimiento.entity.CategoriaArticulo;
import com.industrial.safety.conocimiento.repository.ArticuloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Precarga la Base de Conocimiento con los planes reales del proyecto (curso S16/S31:
 * continuidad, DRP y respaldos; S15/S29: eventos) si la BD esta vacia.
 *
 * <p>Los articulos documentan la plataforma SafeIndustrial tal como es, incluyendo el
 * incidente real del 2026-07-05 (perdida de la cuenta AWS por falta de creditos) y el
 * procedimiento con el que se respaldo y se recuperara. Se puede desactivar con
 * {@code conocimiento.seed.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "conocimiento.seed.enabled", havingValue = "true", matchIfMissing = true)
public class CargaInicialConocimiento implements CommandLineRunner {

    private static final String AUTOR = "Equipo TI SafeIndustrial";

    private final ArticuloRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<Articulo> articulos = List.of(
                planContinuidad(), rtoRpo(), drp(), planRespaldos(), runbookRecuperacion(),
                politicaEventos(), flujoIncidencias());
        for (Articulo a : articulos) {
            Articulo guardado = repository.save(a);
            guardado.setCodigo("KB-%d-%03d".formatted(
                    guardado.getCreatedAt().atZone(ZoneOffset.UTC).getYear(), guardado.getId()));
            repository.save(guardado);
        }
        log.info("[conocimiento] Base de conocimiento precargada con {} articulos", articulos.size());
    }

    private static Articulo articulo(CategoriaArticulo cat, String titulo, String resumen,
                                     String etiquetas, String contenido) {
        return Articulo.builder()
                .categoria(cat).titulo(titulo).resumen(resumen)
                .etiquetas(etiquetas).contenido(contenido).autor(AUTOR)
                .build();
    }

    private static Articulo planContinuidad() {
        return articulo(CategoriaArticulo.CONTINUIDAD,
                "Plan de Continuidad del Servicio (ITSCM) — SafeIndustrial",
                "Servicios críticos de la plataforma, estrategia de alta disponibilidad y resultados esperados del plan de continuidad.",
                "continuidad,itscm,alta disponibilidad,servicios criticos",
                """
                ## Objetivo

                Garantizar que los servicios de TI de SafeIndustrial puedan continuar operando o \
                recuperarse dentro de tiempos aceptables ante incidentes graves o desastres, \
                minimizando el impacto en la operación (ITIL: IT Service Continuity Management).

                ## Fase 1 — Servicios críticos

                | Servicio | Criticidad | Justificación |
                |---|---|---|
                | api-gateway | **Crítica** | Todo el tráfico de la plataforma pasa por él |
                | Keycloak (autenticación) | **Crítica** | Sin login no opera ningún rol |
                | Base de datos PostgreSQL (RDS compartida) | **Crítica** | Única instancia para casi todos los servicios |
                | user-service / safety-service / payment-service | Alta | Identidad, infracciones de seguridad y pagos |
                | course-service / exam-service / purchase-service | Media-Alta | Operación del negocio (cursos, exámenes, EPP) |
                | eventos / incidencias / chat / notification | Media | Soporte interno y comunicación |

                ## Estrategia de alta disponibilidad (Fase 4)

                - **ECS Fargate** con reinicio automático de tasks caídos (autohealing).
                - **Autoscaling 1→4 tasks** validado con prueba de carga real.
                - **Cloud Map** como descubrimiento de servicios (DNS interno).
                - **RabbitMQ con colas durables y DLQ**: ningún mensaje crítico se pierde.
                - **Módulo de Gestión de Eventos** (eventos-service) como detección temprana: \
                los eventos Error/Critical escalan a incidencia automáticamente.
                - Punto único de entrada actual (ngrok) con alternativa documentada: IP pública del gateway :9000.

                ## Resultados esperados

                | Indicador | Antes | Después |
                |---|---|---|
                | RPO de la base de datos | 24 horas | 15 minutos (PITR) |
                | Recuperación de microservicios | Manual | Redeploy automatizado por CI/CD |
                | Copias externas de respaldos | 0 | 1 (regla 3-2-1) |
                | Detección de degradación | Reactiva | Proactiva (umbrales del módulo de eventos) |

                > Ver también: [[RTO y RPO por servicio]], [[Plan de Respaldos y regla 3-2-1]] y \
                [[Plan de Recuperación ante Desastres (DRP)]].
                """);
    }

    private static Articulo rtoRpo() {
        return articulo(CategoriaArticulo.CONTINUIDAD,
                "RTO y RPO por servicio",
                "Tiempos objetivo de recuperación (RTO) y pérdida máxima de datos aceptable (RPO) de cada servicio de la plataforma.",
                "rto,rpo,recuperacion,indicadores",
                """
                ## Definiciones

                - **RTO (Recovery Time Objective)**: tiempo máximo permitido para recuperar un servicio.
                - **RPO (Recovery Point Objective)**: cantidad máxima de información que puede perderse \
                (equivale a la antigüedad del último respaldo utilizable).

                ## Tabla por servicio

                | Servicio | RTO | RPO actual | RPO objetivo | Sustento |
                |---|---|---|---|---|
                | Base de datos (RDS) | 1 hora | **24 h** (snapshot diario 07:00) | 15 min | Activar PITR de RDS |
                | api-gateway y microservicios | 30 min | 0 (stateless) | 0 | Redeploy desde ECR/git vía CI/CD |
                | Keycloak | 1 hora | 24 h | 1 h | Export periódico del realm + task ECS con restart |
                | Configuración (SSM) | 15 min | Al último export | Al último cambio | Export JSON versionado del Parameter Store |
                | Evidencias (S3) | 1 hora | 0 | 0 | Durabilidad nativa de S3 + versioning |

                ## Nota sobre el RPO actual de la BD

                Hoy el respaldo automático es un snapshot diario a las 07:00 UTC: en el peor caso se \
                pierden hasta 24 horas de datos. La acción correctiva es habilitar los backups \
                automáticos con Point-In-Time Recovery (PITR) de RDS, que baja el RPO a ~5-15 minutos \
                sin cambiar la aplicación.
                """);
    }

    private static Articulo drp() {
        return articulo(CategoriaArticulo.DRP,
                "Plan de Recuperación ante Desastres (DRP)",
                "Análisis de riesgos, procedimiento de recuperación y el caso real del 2026-07-05 (pérdida de la cuenta AWS por falta de créditos).",
                "drp,desastre,riesgos,aws,recuperacion",
                """
                ## Objetivo

                Restaurar la infraestructura tecnológica de SafeIndustrial (aplicaciones, base de \
                datos, configuración y comunicaciones) después de un desastre, con un RTO máximo de \
                2 horas para los servicios críticos.

                ## Fase 2 — Análisis de riesgos

                | Riesgo | Probabilidad | Impacto | Mitigación |
                |---|---|---|---|
                | **Agotamiento de créditos / pérdida de cuenta AWS** | Alta (ocurrió el 2026-07-05) | Alto | Alertas de billing + respaldos portables fuera de la cuenta |
                | Caída de la RDS única compartida | Media | Alto | Snapshots + PITR; réplica de lectura a futuro |
                | Ransomware / borrado accidental de datos | Media | Alto | Snapshots + copia externa (regla 3-2-1) |
                | Caída del túnel ngrok (punto único de entrada) | Alta | Alto | Acceso directo a la IP pública del gateway :9000 |
                | Error humano en despliegue | Alta | Medio | CI/CD con task-definition por :sha → rollback inmediato |
                | Indisponibilidad de región AWS | Baja | Alto | Copia de respaldos cross-region |

                ## Caso real: pérdida de la cuenta AWS (2026-07-05)

                La cuenta AWS del proyecto agotó sus créditos y hubo que vaciarla. La respuesta aplicó \
                este DRP en la práctica:

                1. **Respaldo previo a la destrucción**: snapshot manual de RDS \
                (`db-industrial-safety-snapshot`, 20 GB) + export de los 67 parámetros SSM a JSON.
                2. **Teardown controlado**: se eliminó todo lo facturable (ECS, ECR, Cloud Map, logs, \
                alarmas) preservando los snapshots.
                3. **Recuperación planificada**: restaurar el snapshot y reimportar la configuración en \
                la cuenta nueva (ver el runbook [[Restaurar BD y configuración en una cuenta nueva]]).

                **Lección aprendida**: los respaldos quedaron dentro de la misma cuenta en riesgo. \
                La regla 3-2-1 exige al menos una copia externa (otra cuenta o local).

                ## Procedimiento general de recuperación

                1. Detectar el incidente (módulo de eventos / alarmas) y activar al responsable.
                2. Confirmar el alcance del desastre y comunicarlo al equipo.
                3. Aprovisionar la infraestructura destino (cluster ECS, RDS, colas).
                4. Restaurar la base de datos desde el snapshot o réplica más reciente.
                5. Reimportar la configuración (SSM Parameter Store) desde el export versionado.
                6. Redesplegar los microservicios desde ECR/git con el pipeline de CI/CD.
                7. Validar servicios (health checks del actuator + prueba funcional por rol).
                8. Habilitar el acceso de usuarios, monitorear la estabilidad y documentar el incidente.
                """);
    }

    private static Articulo planRespaldos() {
        return articulo(CategoriaArticulo.RESPALDOS,
                "Plan de Respaldos y regla 3-2-1",
                "Qué se respalda, con qué frecuencia y retención; evaluación honesta de la regla 3-2-1 y acciones para cumplirla.",
                "respaldos,backup,3-2-1,snapshot,rds,ssm",
                """
                ## Inventario de respaldos (Fase 5)

                | Recurso | Tipo | Frecuencia | Retención | Estado |
                |---|---|---|---|---|
                | Base de datos (RDS) | Snapshot automático | Diario 07:00 UTC | 7 días | ✅ Activo |
                | Base de datos (RDS) | Snapshot manual | Antes de cambios mayores | Indefinida | ✅ Practicado (2026-07-05) |
                | Parámetros SSM (configuración) | Export JSON completo | En cada cambio relevante | Versionado | ✅ Practicado (67 parámetros) |
                | Imágenes Docker | Registro ECR + reconstruibles desde git | En cada push (CI) | Última + historial git | ✅ Activo |
                | Código fuente | GitHub (backend + frontend) | En cada commit | Historial completo | ✅ Activo |
                | Evidencias de usuarios | S3 | Continuo | Según bucket | ⚠️ Sin versioning |

                ## Evaluación de la regla 3-2-1

                La regla exige **3** copias de los datos, en **2** medios distintos, con **1** copia \
                fuera del sitio.

                | Criterio | Situación actual | Cumple |
                |---|---|---|
                | 3 copias | BD: snapshot automático + snapshot manual (2 copias) | ❌ |
                | 2 medios distintos | Todo dentro de la misma cuenta AWS | ❌ |
                | 1 copia externa | Ninguna (los snapshots viven en la cuenta en riesgo) | ❌ |

                **El incidente del 2026-07-05 lo demostró**: si AWS hubiera cerrado la cuenta \
                inmediatamente, los snapshots se habrían perdido con ella.

                ## Acciones para cumplir 3-2-1

                1. Exportar un `pg_dump` de la BD a un S3 de **otra cuenta** (o almacenamiento local) \
                después de cada snapshot manual.
                2. Mantener el export JSON de SSM **fuera de AWS** (repositorio privado o disco local cifrado).
                3. Habilitar versioning en el bucket de evidencias.
                4. Probar la restauración periódicamente (la migración de cuenta pendiente es la \
                primera prueba real).
                """);
    }

    private static Articulo runbookRecuperacion() {
        return articulo(CategoriaArticulo.RUNBOOK,
                "Runbook: restaurar BD y configuración en una cuenta nueva",
                "Procedimiento paso a paso para recuperar la plataforma en otra cuenta AWS desde el snapshot RDS y el export de SSM.",
                "runbook,migracion,restore,snapshot,ssm,aws cli",
                """
                ## Prerrequisitos

                - Snapshot RDS compartido o copiado a la cuenta destino (`db-industrial-safety-snapshot`).
                - Export JSON de los parámetros SSM (formato `get-parameters-by-path`).
                - Credenciales CLI de la cuenta destino y pipeline CI/CD apuntando a su ECR.

                ## Paso 1 — Compartir el snapshot con la cuenta destino

                ```bash
                aws rds modify-db-snapshot-attribute \\
                  --db-snapshot-identifier db-industrial-safety-snapshot \\
                  --attribute-name restore --values-to-add <CUENTA_DESTINO>
                ```

                ## Paso 2 — Restaurar la base de datos (en la cuenta destino)

                ```bash
                aws rds restore-db-instance-from-db-snapshot \\
                  --db-instance-identifier db-industrial-safety \\
                  --db-snapshot-identifier arn:del:snapshot:compartido \\
                  --db-instance-class db.t4g.micro --no-multi-az
                ```

                ## Paso 3 — Reimportar la configuración SSM desde el JSON

                ```bash
                # Por cada parametro del export:
                aws ssm put-parameter --name "<Name>" --value "<Value>" \\
                  --type SecureString --overwrite
                ```

                Actualizar los parámetros que cambian con la cuenta: URL de la BD, host de RabbitMQ, \
                issuer de Keycloak y token de ngrok.

                ## Paso 4 — Redesplegar los servicios

                1. Recrear cluster ECS, namespace de Cloud Map y repositorios ECR (o vía IaC).
                2. Ejecutar el pipeline de CI/CD: reconstruye las imágenes desde git y registra las \
                task-definitions por `:sha`.

                ## Paso 5 — Validación (checklist)

                - [ ] `actuator/health` responde UP en todos los servicios.
                - [ ] Login por Keycloak funciona para cada rol.
                - [ ] Una incidencia de prueba fluye: evento → incidencia → resolución.
                - [ ] Los datos históricos (usuarios, cursos, incidencias) están presentes.

                > Este runbook nació del incidente real del 2026-07-05 (ver \
                [[Plan de Recuperación ante Desastres (DRP)]]).
                """);
    }

    private static Articulo politicaEventos() {
        return articulo(CategoriaArticulo.EVENTOS,
                "Política de umbrales y clasificación de eventos",
                "Cómo el módulo de Gestión de Eventos clasifica en Información/Warning/Error/Critical y cuándo un evento escala a incidencia.",
                "eventos,umbrales,monitoreo,clasificacion,politicas",
                """
                ## Niveles de evento (S15/S29)

                | Nivel | Descripción | Acción | ¿Genera incidente? |
                |---|---|---|---|
                | Información | Evento normal | Registrar | No |
                | Warning | Riesgo potencial | Monitorear | No |
                | Error | Falla parcial | Intervenir | **Sí** |
                | Critical | Servicio interrumpido | Atención inmediata | **Sí** |

                ## Umbrales por métrica (política por defecto)

                | Métrica | Información | Warning | Error | Critical |
                |---|---|---|---|---|
                | CPU (%) | 0-74 | 75-84 | 85-94 | 95+ |
                | RAM (%) | 0-74 | 75-84 | 85-94 | 95+ |
                | Disco (%) | 0-79 | 80-89 | 90-94 | 95+ |
                | Logins fallidos (conteo) | 0-4 | 5-14 | 15-29 | 30+ |
                | Latencia BD (ms) | 0-499 | 500-1499 | 1500-4999 | 5000+ |

                Los umbrales son configurables sin recompilar (application.yaml o SSM \
                `/config/eventos-service/`). Los eventos textuales sin valor numérico se clasifican \
                por palabras clave (ej. "deja de responder" → Critical).

                ## Flujo evento → incidencia

                1. Un servicio (o el simulador) publica el evento a `POST /api/v1/eventos`.
                2. eventos-service lo clasifica por umbral y lo persiste.
                3. Si es **Error/Critical**, se publica a RabbitMQ y el servicio de incidencias crea \
                la incidencia automáticamente (fuente EVENTO, prioridad por matriz impacto×urgencia).
                4. La incidencia confirmada se enlaza al evento (columna "Incidente" del tablero).

                > El tablero vive en **Soporte → Eventos**; la política activa se consulta con el \
                botón "Políticas".
                """);
    }

    private static Articulo flujoIncidencias() {
        return articulo(CategoriaArticulo.INCIDENCIAS,
                "Flujo de atención de incidencias TI",
                "Ciclo de vida de una incidencia, matriz de priorización impacto×urgencia y responsabilidades del rol Soporte.",
                "incidencias,prioridad,itil,soporte,flujo",
                """
                ## Ciclo de vida

                `REGISTRADO → EN_ATENCION → RESUELTO → CERRADO`

                - **Registrado**: la reportó un usuario (con triaje automático por reglas/IA) o la \
                generó un evento de monitoreo.
                - **En atención**: un agente de Soporte la aceptó (queda como responsable).
                - **Resuelto**: se documentó la solución y si quedó conforme.

                ## Matriz de priorización (impacto × urgencia)

                | Impacto \\ Urgencia | Alto | Medio | Bajo |
                |---|---|---|---|
                | **Alto** | Crítica | Alta | Media |
                | **Medio** | Alta | Media | Baja |
                | **Bajo** | Media | Baja | Baja |

                Las incidencias generadas por eventos llegan pre-priorizadas: Critical → impacto Alto \
                (prioridad **Crítica**); Error → impacto Medio (prioridad **Alta**).

                ## Responsabilidades del rol Soporte

                1. Atender el tablero por prioridad (Crítica primero).
                2. Aceptar la incidencia antes de trabajarla (trazabilidad de responsable).
                3. Documentar la resolución al cerrar (alimenta esta base de conocimiento).
                4. Sincronizar con la mesa de ayuda externa (Jira) cuando aplique.
                5. Ante incidencias repetidas, proponer un artículo nuevo aquí (aprendizaje continuo).
                """);
    }
}
