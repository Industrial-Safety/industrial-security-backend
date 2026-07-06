package com.industrial.safety.incidencias.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro central de una incidencia de TI de la plataforma.
 * NO confundir con {@code incident} (infraccion de seguridad por camara).
 */
@Entity
@Table(name = "incidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Codigo legible, ej. INC-2026-001. Se asigna tras persistir. */
    @Column(unique = true)
    private String codigo;

    // ── Quien reporta ────────────────────────────────────────────────
    private String reporterId;     // keycloakId
    private String reporterName;
    private String reporterRole;

    // ── Origen ───────────────────────────────────────────────────────
    /** USUARIO (reportada por una persona) o EVENTO (generada por una alarma de monitoreo). */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FuenteIncidencia fuente = FuenteIncidencia.USUARIO;

    // ── Clasificacion ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    /** Subtipo segun el rol, ej. "Video no carga". */
    private String tipo;

    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    // ── Priorizacion ─────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private Nivel impacto;

    @Enumerated(EnumType.STRING)
    private Nivel urgencia;

    /** Calculada en servidor a partir de impacto x urgencia. Nunca se acepta del cliente. */
    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;

    // ── Clasificacion asistida (reglas / IA) ─────────────────────────
    /** De donde salio la categoria: REGLA (fallback), IA (Bedrock) o HUMANO (soporte). */
    @Enumerated(EnumType.STRING)
    private OrigenClasificacion categoriaOrigen;

    /** true = clasificacion provisional que soporte deberia revisar. */
    private Boolean requiereRevision;

    /** Confianza de la IA en su clasificacion (0.0–1.0). Null si no la clasifico la IA. */
    private Double iaConfianza;

    /** Diagnostico en lenguaje claro que la IA da a soporte (que paso y por que). */
    @Column(length = 2000)
    private String iaDiagnostico;

    // ── Evidencia (URLs en S3) ───────────────────────────────────────
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incidencia_evidencias", joinColumns = @JoinColumn(name = "incidencia_id"))
    @Column(name = "url", length = 1000)
    @Builder.Default
    private List<String> evidenciaUrls = new ArrayList<>();

    /** Log/registro capturado del navegador al momento del fallo (JSON). Insumo del triaje. */
    @Column(length = 4000)
    private String contextoError;

    // ── SLA de resolucion (RTO aplicado a la atencion, curso S16/S31) ─
    /** Minutos de SLA aplicados segun la prioridad al registrar. */
    private Integer slaMinutos;

    /** Limite de resolucion (createdAt + slaMinutos). El tablero corre el contador contra esta hora. */
    private Instant slaVencimiento;

    /** true = se resolvio dentro del SLA. Null mientras siga abierta. */
    private Boolean slaCumplido;

    /** Justificacion obligatoria cuando se resuelve fuera del SLA (queda registrada para auditoria). */
    @Column(length = 2000)
    private String demoraJustificacion;

    // ── Ciclo de vida ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private EstadoIncidencia estado;

    private String atendidoPor;     // keycloakId del admin/TI
    private Instant aceptadoEn;

    @Column(length = 2000)
    private String resolucionDescripcion;

    private Boolean resueltoBien;
    private Instant resueltoEn;

    /** Opcional/reservado: enlace a un hilo de chat para seguimiento. */
    private String conversationId;

    // ── Sincronización externa (Freshservice) ────────────────────────
    private Long freshserviceTicketId;
    private String freshserviceUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncEstado syncEstado = SyncEstado.NO_SINCRONIZADO;

    @Column(length = 1000)
    private String syncError;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.syncEstado == null) {
            this.syncEstado = SyncEstado.NO_SINCRONIZADO;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
