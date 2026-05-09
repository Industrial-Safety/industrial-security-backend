package com.industrial.safety.order_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_order_number", columnList = "orderNumber", unique = true),
                @Index(name = "idx_orders_user_id", columnList = "userId"),
                @Index(name = "idx_orders_status", columnList = "orderStatus")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private String userId;

    private String userEmail;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus;

    private String paymentIntentId;

    private String receiptUrl;

    private String failureReason;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant paidAt;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderLineItems> orderLineItemsList;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.orderStatus == null) {
            this.orderStatus = OrderStatus.PENDING;
        }
        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "USD";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
