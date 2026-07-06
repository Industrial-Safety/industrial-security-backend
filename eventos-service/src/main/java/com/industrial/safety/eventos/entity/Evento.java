package com.industrial.safety.eventos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Evento de monitoreo detectado en un servicio de la plataforma.
 *
 * <p>Es la entidad de primer nivel del modulo de Gestion de Eventos: se registra,
 * se clasifica por umbrales en {@link NivelEvento} (Informacion/Warning/Error/Critical)
 * y solo los ERROR/CRITICAL escalan a una incidencia. NO se confunde con una incidencia
 * (que vive en incidencias-service): un evento es un cambio de estado observado.
 */
@Entity
@Table(name = "eventos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Codigo legible, ej. EVT-2026-0001. Se asigna tras persistir. */
    @Column(unique = true)
    private String codigo;

    /** Momento en que ocurrio el evento (no el de registro). */
    private Instant ocurridoEn;

    // ── Origen ───────────────────────────────────────────────────────
    /** Servicio o componente que emitio el evento, ej. "api-gateway", "safety-service". */
    private String servicioOrigen;

    /** Metrica o senal observada, ej. "cpu", "ram", "disco", "login_fallidos", "servidor". */
    private String metrica;

    /** Valor numerico observado (%, ms, conteo). Null en eventos puramente textuales. */
    private Double valor;

    @Column(length = 1000)
    private String mensaje;

    // ── Clasificacion (motor de umbrales/politicas) ──────────────────
    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    private NivelEvento nivel;

    /** Regla de umbral que decidio el nivel, ej. "cpu >= 95 -> CRITICAL". Trazabilidad para soporte. */
    private String umbralAplicado;

    // ── Escalamiento a incidencia ────────────────────────────────────
    /** true = el nivel (ERROR/CRITICAL) genero/deberia generar una incidencia. */
    @Builder.Default
    private Boolean generaIncidente = false;

    /** Codigo de la incidencia generada en incidencias-service, si aplica. */
    private String incidenciaCodigo;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.ocurridoEn == null) {
            this.ocurridoEn = this.createdAt;
        }
    }
}
