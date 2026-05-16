package com.industrial.safety.order_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table (name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNumber;

    private String userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;


    @OneToMany(cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderLineItems> orderLineItemsList;




}
