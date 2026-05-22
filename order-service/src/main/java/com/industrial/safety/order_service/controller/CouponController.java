package com.industrial.safety.order_service.controller;

import com.industrial.safety.order_service.dto.CouponRequest;
import com.industrial.safety.order_service.dto.CouponResponse;
import com.industrial.safety.order_service.dto.CouponValidateResponse;
import com.industrial.safety.order_service.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public List<CouponResponse> getAll() {
        return couponService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CouponRequest request) {
        return couponService.create(request);
    }

    @PatchMapping("/{id}/toggle")
    public CouponResponse toggleStatus(@PathVariable Long id) {
        return couponService.toggleStatus(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        couponService.delete(id);
    }

    // GET /api/v1/orders/coupons/preview?code=EPP20&courseId=abc&amount=89.99
    @GetMapping("/preview")
    public CouponValidateResponse preview(
            @RequestParam String code,
            @RequestParam(required = false) String courseId,
            @RequestParam BigDecimal amount) {
        return couponService.preview(code, courseId, amount);
    }
}
