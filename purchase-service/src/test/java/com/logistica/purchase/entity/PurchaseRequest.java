package com.logistica.purchase.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "purchase_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigoSolicitud;

    private LocalDate fecha;

    private String categoria;

    private Integer cantidad;

    private String proveedor;

    private Double costoEstimado;

    @Column(length = 1000)
    private String justificacion;

    private String estado;
}