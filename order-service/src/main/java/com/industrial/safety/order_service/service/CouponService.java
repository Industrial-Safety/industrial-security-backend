package com.industrial.safety.order_service.service;

import com.industrial.safety.order_service.dto.CouponRequest;
import com.industrial.safety.order_service.dto.CouponResponse;
import com.industrial.safety.order_service.dto.CouponValidateResponse;
import com.industrial.safety.order_service.models.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    List<CouponResponse> getAll();
    CouponResponse create(CouponRequest request);
    CouponResponse toggleStatus(Long id);
    void delete(Long id);

    // Used internally by OrderService — returns the locked Coupon entity
    Coupon validateAndGet(String code, String courseId);

    // Preview endpoint: returns discount preview without consuming any use
    CouponValidateResponse preview(String code, String courseId, BigDecimal amount);

    // Called after confirmed payment to consume one use
    void consumeUse(String code);
}
