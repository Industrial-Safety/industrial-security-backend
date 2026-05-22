package com.industrial.safety.order_service.dto;

import com.industrial.safety.order_service.models.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
        @NotBlank @Size(min = 3, max = 50) String code,
        @NotNull DiscountType discountType,
        @NotNull @Positive @DecimalMax("100.00") BigDecimal value,
        @Positive Integer maxUses,
        String courseId,
        Instant expiryDate,
        String createdByUserId,
        String createdByName
) {}
