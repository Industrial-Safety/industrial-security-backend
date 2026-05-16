package com.logistica.purchase.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "epp_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EppDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long inventoryItemId;
    private String inventoryItemDescripcion;
    private String workerDni;
    private String workerName;
    private Integer cantidadEntregada;
    private LocalDate fechaEntrega;
}
