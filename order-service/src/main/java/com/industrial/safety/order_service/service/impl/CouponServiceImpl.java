package com.industrial.safety.order_service.service.impl;

import com.industrial.safety.order_service.dto.CouponRequest;
import com.industrial.safety.order_service.dto.CouponResponse;
import com.industrial.safety.order_service.dto.CouponValidateResponse;
import com.industrial.safety.order_service.exception.InvalidCouponException;
import com.industrial.safety.order_service.exception.ResourceNotFoundException;
import com.industrial.safety.order_service.models.Coupon;
import com.industrial.safety.order_service.models.CouponStatus;
import com.industrial.safety.order_service.repository.CouponRepository;
import com.industrial.safety.order_service.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAll() {
        return couponRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CouponResponse create(CouponRequest req) {
        if (couponRepository.findByCode(req.code().toUpperCase()).isPresent()) {
            throw new InvalidCouponException("El código '" + req.code() + "' ya existe");
        }
        Coupon coupon = Coupon.builder()
                .code(req.code().toUpperCase())
                .discountType(req.discountType())
                .value(req.value())
                .maxUses(req.maxUses())
                .courseId(req.courseId())
                .expiryDate(req.expiryDate())
                .createdByUserId(req.createdByUserId())
                .createdByName(req.createdByName())
                .build();
        return toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse toggleStatus(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        if (coupon.getStatus() == CouponStatus.EXHAUSTED) {
            throw new InvalidCouponException("No se puede reactivar un cupón agotado");
        }
        coupon.setStatus(coupon.getStatus() == CouponStatus.ACTIVE
                ? CouponStatus.DISABLED
                : CouponStatus.ACTIVE);
        return toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon", "id", id);
        }
        couponRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Coupon validateAndGet(String code, String courseId) {
        Coupon coupon = couponRepository.findByCodeForUpdate(code.toUpperCase())
                .orElseThrow(() -> new InvalidCouponException("Cupón '" + code + "' no existe"));
        if (!coupon.isUsable(courseId)) {
            throw new InvalidCouponException("El cupón '" + code + "' no es válido o ha expirado");
        }
        return coupon;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidateResponse preview(String code, String courseId, BigDecimal amount) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new InvalidCouponException("Cupón '" + code + "' no existe"));
        if (!coupon.isUsable(courseId)) {
            throw new InvalidCouponException("El cupón '" + code + "' no es válido o ha expirado");
        }
        BigDecimal discount = coupon.discountAmount(amount);
        BigDecimal finalAmount = coupon.applyTo(amount);
        return new CouponValidateResponse(coupon.getCode(), coupon.getDiscountType(),
                coupon.getValue(), amount, discount, finalAmount);
    }

    @Override
    @Transactional
    public void consumeUse(String code) {
        Coupon coupon = couponRepository.findByCodeForUpdate(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));
        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        if (coupon.getMaxUses() != null && coupon.getCurrentUses() >= coupon.getMaxUses()) {
            coupon.setStatus(CouponStatus.EXHAUSTED);
            log.info("Coupon {} exhausted after {} uses", coupon.getCode(), coupon.getCurrentUses());
        }
        couponRepository.save(coupon);
    }

    private CouponResponse toResponse(Coupon c) {
        return new CouponResponse(c.getId(), c.getCode(), c.getDiscountType(), c.getValue(),
                c.getMaxUses(), c.getCurrentUses(), c.getCourseId(), c.getExpiryDate(),
                c.getStatus(), c.getCreatedByUserId(), c.getCreatedByName(), c.getCreatedAt());
    }
}
