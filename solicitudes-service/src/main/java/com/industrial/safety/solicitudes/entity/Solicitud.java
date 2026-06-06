package com.industrial.safety.solicitudes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro central de cada solicitud recibida desde cualquier microservicio.
 * Guarda la trazabilidad: tipo ITIL, origen, estado y la clave del ticket en Jira.
 */
@Entity
@Table(name = "solicitudes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String codigo;

    /** SERVICIO | INFORMACION | ACCESO */
    private String tipo;

    private String subtipo;

    private String solicitante;

    private String microservicioOrigen;

    private String prioridad;

    @Column(length = 1000)
    private String descripcion;

    /** REGISTRADA | EN_JIRA | ERROR_JIRA */
    @Enumerated(EnumType.STRING)
    private SolicitudStatus estado;

    /** Clave del ticket creado en Jira, ej. GSI-12. */
    private String jiraKey;

    private LocalDate fechaSolicitud;

    private LocalDateTime fechaRegistro;
}
