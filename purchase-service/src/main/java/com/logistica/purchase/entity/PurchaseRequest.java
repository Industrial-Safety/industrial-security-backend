package com.logistica.purchase.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "purchase_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String codigoSolicitud;

    private LocalDate fecha;

    private String categoria;

    private Integer cantidad;

    private String proveedor;

    private Double costoEstimado;

    @Column(length = 1000)
    private String justificacion;

    @Enumerated(EnumType.STRING)
    private PurchaseRequestStatus estado;
}
