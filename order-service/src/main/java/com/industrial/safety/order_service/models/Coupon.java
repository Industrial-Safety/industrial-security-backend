package com.industrial.safety.order_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "coupons",
    indexes = {
        @Index(name = "idx_coupon_code", columnList = "code", unique = true),
        @Index(name = "idx_coupon_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal value;

    // null = unlimited
    private Integer maxUses;

    @Builder.Default
    private Integer currentUses = 0;

    // null = applies to all courses
    private String courseId;

    private Instant expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    private String createdByUserId;
    private String createdByName;

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.currentUses == null) this.currentUses = 0;
        if (this.status == null) this.status = CouponStatus.ACTIVE;
    }

    public boolean isUsable(String requestedCourseId) {
        if (status != CouponStatus.ACTIVE) return false;
        if (expiryDate != null && Instant.now().isAfter(expiryDate)) return false;
        if (maxUses != null && currentUses >= maxUses) return false;
        if (courseId != null && !courseId.equals(requestedCourseId)) return false;
        return true;
    }

    public BigDecimal applyTo(BigDecimal amount) {
        if (discountType == DiscountType.PERCENTAGE) {
            BigDecimal factor = value.divide(BigDecimal.valueOf(100));
            BigDecimal discount = amount.multiply(factor);
            return amount.subtract(discount).max(BigDecimal.ZERO);
        }
        return amount.subtract(value).max(BigDecimal.ZERO);
    }

    public BigDecimal discountAmount(BigDecimal amount) {
        return amount.subtract(applyTo(amount));
    }
}
