package com.industrial.safety.order_service.dto;

import com.industrial.safety.order_service.models.DiscountType;

import java.math.BigDecimal;

public record CouponValidateResponse(
        String code,
        DiscountType discountType,
        BigDecimal value,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {}
