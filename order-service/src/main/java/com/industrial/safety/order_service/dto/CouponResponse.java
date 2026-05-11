package com.industrial.safety.order_service.dto;

import com.industrial.safety.order_service.models.CouponStatus;
import com.industrial.safety.order_service.models.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal value,
        Integer maxUses,
        Integer currentUses,
        String courseId,
        Instant expiryDate,
        CouponStatus status,
        String createdByUserId,
        String createdByName,
        Instant createdAt
) {}
