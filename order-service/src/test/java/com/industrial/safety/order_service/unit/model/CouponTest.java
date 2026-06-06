package com.industrial.safety.order_service.unit.model;

import com.industrial.safety.order_service.models.Coupon;
import com.industrial.safety.order_service.models.CouponStatus;
import com.industrial.safety.order_service.models.DiscountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Coupon — Pruebas Unitarias")
class CouponTest {

    private Coupon.CouponBuilder activeBuilder() {
        return Coupon.builder()
                .code("PROMO")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .currentUses(0)
                .status(CouponStatus.ACTIVE);
    }

    @Test
    @DisplayName("isUsable: cupón ACTIVE sin límites es usable")
    void isUsable_activeNoLimits_true() {
        assertThat(activeBuilder().build().isUsable("any-course")).isTrue();
    }

    @Test
    @DisplayName("isUsable: estado no ACTIVE -> false")
    void isUsable_notActive_false() {
        assertThat(activeBuilder().status(CouponStatus.DISABLED).build().isUsable("c1")).isFalse();
    }

    @Test
    @DisplayName("isUsable: cupón expirado -> false")
    void isUsable_expired_false() {
        Coupon c = activeBuilder().expiryDate(Instant.now().minus(1, ChronoUnit.DAYS)).build();
        assertThat(c.isUsable("c1")).isFalse();
    }

    @Test
    @DisplayName("isUsable: fecha de expiración futura -> sigue usable")
    void isUsable_notExpired_true() {
        Coupon c = activeBuilder().expiryDate(Instant.now().plus(1, ChronoUnit.DAYS)).build();
        assertThat(c.isUsable("c1")).isTrue();
    }

    @Test
    @DisplayName("isUsable: usos agotados -> false")
    void isUsable_maxUsesReached_false() {
        Coupon c = activeBuilder().maxUses(2).currentUses(2).build();
        assertThat(c.isUsable("c1")).isFalse();
    }

    @Test
    @DisplayName("isUsable: curso distinto al del cupón -> false")
    void isUsable_courseMismatch_false() {
        Coupon c = activeBuilder().courseId("course-A").build();
        assertThat(c.isUsable("course-B")).isFalse();
    }

    @Test
    @DisplayName("isUsable: curso coincide -> true")
    void isUsable_courseMatch_true() {
        Coupon c = activeBuilder().courseId("course-A").build();
        assertThat(c.isUsable("course-A")).isTrue();
    }

    @Test
    @DisplayName("applyTo: descuento porcentual")
    void applyTo_percentage() {
        Coupon c = activeBuilder().discountType(DiscountType.PERCENTAGE).value(new BigDecimal("10")).build();
        assertThat(c.applyTo(new BigDecimal("100"))).isEqualByComparingTo("90");
    }

    @Test
    @DisplayName("applyTo: descuento fijo")
    void applyTo_fixed() {
        Coupon c = activeBuilder().discountType(DiscountType.FIXED).value(new BigDecimal("15")).build();
        assertThat(c.applyTo(new BigDecimal("100"))).isEqualByComparingTo("85");
    }

    @Test
    @DisplayName("applyTo: descuento fijo mayor al total -> no baja de cero")
    void applyTo_fixedFlooredAtZero() {
        Coupon c = activeBuilder().discountType(DiscountType.FIXED).value(new BigDecimal("500")).build();
        assertThat(c.applyTo(new BigDecimal("100"))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("discountAmount: total - aplicado")
    void discountAmount_percentage() {
        Coupon c = activeBuilder().discountType(DiscountType.PERCENTAGE).value(new BigDecimal("10")).build();
        assertThat(c.discountAmount(new BigDecimal("100"))).isEqualByComparingTo("10");
    }
}
