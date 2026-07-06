package com.industrial.safety.conocimiento.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Articulo de la Base de Conocimiento (ITIL Knowledge Management).
 *
 * <p>Aqui viven los planes (continuidad, DRP, respaldos), politicas y runbooks del
 * proyecto para que el rol de Soporte TI los consulte y aprenda de ellos. El contenido
 * es Markdown y lo renderiza el frontend.
 */
@Entity
@Table(name = "articulos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Codigo legible, ej. KB-2026-001. Se asigna tras persistir. */
    @Column(unique = true)
    private String codigo;

    private String titulo;

    /** Resumen corto para el listado (el contenido completo solo se carga al abrir). */
    @Column(length = 500)
    private String resumen;

    @Enumerated(EnumType.STRING)
    private CategoriaArticulo categoria;

    /** Cuerpo del articulo en Markdown. */
    @Column(columnDefinition = "TEXT")
    private String contenido;

    /** Etiquetas separadas por coma, ej. "drp,aws,rds". Se usan en la busqueda. */
    private String etiquetas;

    private String autor;

    /** Veces que soporte abrio el articulo (senal de que conocimiento se consulta mas). */
    @Builder.Default
    private Long vistas = 0L;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.vistas == null) {
            this.vistas = 0L;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
