package com.industrial.safety.order_service.models;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity (name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderService
{

    private String id;


}
