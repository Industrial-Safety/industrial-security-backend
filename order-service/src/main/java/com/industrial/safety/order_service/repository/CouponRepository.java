package com.industrial.safety.order_service.repository;

import com.industrial.safety.order_service.models.Coupon;
import com.industrial.safety.order_service.models.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // Pessimistic lock para evitar doble uso en pagos concurrentes
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeForUpdate(String code);

    Optional<Coupon> findByCode(String code);

    List<Coupon> findAllByOrderByCreatedAtDesc();

    List<Coupon> findByStatus(CouponStatus status);
}
