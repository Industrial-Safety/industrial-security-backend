package com.industrial.safety.order_service.unit.service;

import com.industrial.safety.order_service.dto.CouponRequest;
import com.industrial.safety.order_service.exception.InvalidCouponException;
import com.industrial.safety.order_service.exception.ResourceNotFoundException;
import com.industrial.safety.order_service.models.Coupon;
import com.industrial.safety.order_service.models.CouponStatus;
import com.industrial.safety.order_service.models.DiscountType;
import com.industrial.safety.order_service.repository.CouponRepository;
import com.industrial.safety.order_service.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl — Pruebas Unitarias")
class CouponServiceImplTest {

    @Mock CouponRepository couponRepository;

    @InjectMocks CouponServiceImpl couponService;

    private Coupon activeCoupon;

    @BeforeEach
    void setUp() {
        activeCoupon = Coupon.builder()
                .id(1L)
                .code("DESCUENTO20")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("20"))
                .maxUses(100)
                .currentUses(5)
                .status(CouponStatus.ACTIVE)
                .build();
    }

    // =========================================================
    //  getAll
    // =========================================================

    @Test
    @DisplayName("getAll: retorna lista de cupones mapeados")
    void getAll_returnsList() {
        given(couponRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(activeCoupon));

        var result = couponService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("DESCUENTO20");
    }

    // =========================================================
    //  create
    // =========================================================

    @Test
    @DisplayName("create: crea un cupón nuevo con código en mayúsculas")
    void create_newCode_success() {
        var req = new CouponRequest("desc10", DiscountType.PERCENTAGE, new BigDecimal("10"),
                50, null, null, "admin-1", "Admin");

        given(couponRepository.findByCode("DESC10")).willReturn(Optional.empty());
        given(couponRepository.save(any(Coupon.class))).willReturn(activeCoupon);

        var result = couponService.create(req);

        assertThat(result).isNotNull();
        then(couponRepository).should().save(any(Coupon.class));
    }

    @Test
    @DisplayName("create: lanza InvalidCouponException si el código ya existe")
    void create_duplicateCode_throws() {
        var req = new CouponRequest("DESCUENTO20", DiscountType.PERCENTAGE, new BigDecimal("20"),
                100, null, null, "admin-1", "Admin");

        given(couponRepository.findByCode("DESCUENTO20")).willReturn(Optional.of(activeCoupon));

        assertThatThrownBy(() -> couponService.create(req))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("DESCUENTO20");
    }

    // =========================================================
    //  toggleStatus
    // =========================================================

    @Test
    @DisplayName("toggleStatus: ACTIVE → DISABLED")
    void toggleStatus_activeToDisabled() {
        activeCoupon.setStatus(CouponStatus.ACTIVE);
        given(couponRepository.findById(1L)).willReturn(Optional.of(activeCoupon));
        given(couponRepository.save(activeCoupon)).willReturn(activeCoupon);

        var result = couponService.toggleStatus(1L);

        assertThat(activeCoupon.getStatus()).isEqualTo(CouponStatus.DISABLED);
    }

    @Test
    @DisplayName("toggleStatus: DISABLED → ACTIVE")
    void toggleStatus_disabledToActive() {
        activeCoupon.setStatus(CouponStatus.DISABLED);
        given(couponRepository.findById(1L)).willReturn(Optional.of(activeCoupon));
        given(couponRepository.save(activeCoupon)).willReturn(activeCoupon);

        couponService.toggleStatus(1L);

        assertThat(activeCoupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
    }

    @Test
    @DisplayName("toggleStatus: EXHAUSTED → lanza InvalidCouponException")
    void toggleStatus_exhausted_throws() {
        activeCoupon.setStatus(CouponStatus.EXHAUSTED);
        given(couponRepository.findById(1L)).willReturn(Optional.of(activeCoupon));

        assertThatThrownBy(() -> couponService.toggleStatus(1L))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("agotado");
    }

    @Test
    @DisplayName("toggleStatus: lanza ResourceNotFoundException si el ID no existe")
    void toggleStatus_notFound_throws() {
        given(couponRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.toggleStatus(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================
    //  delete
    // =========================================================

    @Test
    @DisplayName("delete: elimina el cupón exitosamente")
    void delete_existingId() {
        given(couponRepository.existsById(1L)).willReturn(true);

        couponService.delete(1L);

        then(couponRepository).should().deleteById(1L);
    }

    @Test
    @DisplayName("delete: lanza ResourceNotFoundException si el ID no existe")
    void delete_notFound_throws() {
        given(couponRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> couponService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================
    //  validateAndGet
    // =========================================================

    @Test
    @DisplayName("validateAndGet: retorna cupón válido y usable")
    void validateAndGet_valid() {
        given(couponRepository.findByCodeForUpdate("DESCUENTO20")).willReturn(Optional.of(activeCoupon));

        Coupon result = couponService.validateAndGet("DESCUENTO20", null);

        assertThat(result.getCode()).isEqualTo("DESCUENTO20");
    }

    @Test
    @DisplayName("validateAndGet: lanza excepción si el código no existe")
    void validateAndGet_codeNotFound_throws() {
        given(couponRepository.findByCodeForUpdate("NOEXISTE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.validateAndGet("NOEXISTE", null))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("NOEXISTE");
    }

    @Test
    @DisplayName("validateAndGet: lanza excepción si el cupón está DISABLED")
    void validateAndGet_disabledCoupon_throws() {
        activeCoupon.setStatus(CouponStatus.DISABLED);
        given(couponRepository.findByCodeForUpdate("DESCUENTO20")).willReturn(Optional.of(activeCoupon));

        assertThatThrownBy(() -> couponService.validateAndGet("DESCUENTO20", null))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    @DisplayName("validateAndGet: lanza excepción si el cupón está expirado")
    void validateAndGet_expiredCoupon_throws() {
        activeCoupon.setExpiryDate(Instant.now().minusSeconds(3600)); // expiró hace 1 hora
        given(couponRepository.findByCodeForUpdate("DESCUENTO20")).willReturn(Optional.of(activeCoupon));

        assertThatThrownBy(() -> couponService.validateAndGet("DESCUENTO20", null))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    @DisplayName("validateAndGet: lanza excepción si el cupón es de curso específico y no coincide")
    void validateAndGet_wrongCourse_throws() {
        activeCoupon.setCourseId("course-A");
        given(couponRepository.findByCodeForUpdate("DESCUENTO20")).willReturn(Optional.of(activeCoupon));

        assertThatThrownBy(() -> couponService.validateAndGet("DESCUENTO20", "course-B"))
                .isInstanceOf(InvalidCouponException.class);
    }

    // =========================================================
    //  consumeUse
    // =========================================================

    @Test
    @DisplayName("consumeUse: incrementa currentUses")
    void consumeUse_incrementsUses() {
        activeCoupon.setCurrentUses(5);
        activeCoupon.setMaxUses(100);
        given(couponRepository.findByCodeForUpdate("DESCUENTO20")).willReturn(Optional.of(activeCoupon));

        couponService.consumeUse("DESCUENTO20");

        assertThat(activeCoupon.getCurrentUses()).isEqualTo(6);
        then(couponRepository).should().save(activeCoupon);
    }

    @Test
    @DisplayName("consumeUse: marca EXHAUSTED cuando se alcanza el límite de usos")
    void consumeUse_marksExhaustedWhenMaxReached() {
        activeCoupon.setCurrentUses(99);
        activeCoupon.setMaxUses(100);
        given(couponRepository.findByCodeForUpdate("DESCUENTO20")).willReturn(Optional.of(activeCoupon));

        couponService.consumeUse("DESCUENTO20");

        assertThat(activeCoupon.getStatus()).isEqualTo(CouponStatus.EXHAUSTED);
        assertThat(activeCoupon.getCurrentUses()).isEqualTo(100);
    }

    @Test
    @DisplayName("consumeUse: no marca EXHAUSTED con maxUses null (ilimitado)")
    void consumeUse_unlimitedUses_neverExhausted() {
        activeCoupon.setCurrentUses(1000);
        activeCoupon.setMaxUses(null);
        given(couponRepository.findByCodeForUpdate("DESCUENTO20")).willReturn(Optional.of(activeCoupon));

        couponService.consumeUse("DESCUENTO20");

        assertThat(activeCoupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
    }
}
