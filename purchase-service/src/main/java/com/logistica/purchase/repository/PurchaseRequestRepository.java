package com.logistica.purchase.repository;

import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.entity.PurchaseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    long countByEstado(PurchaseRequestStatus estado);

    List<PurchaseRequest> findByEstado(PurchaseRequestStatus estado);

    @Query("SELECT COALESCE(SUM(p.costoEstimado), 0.0) FROM PurchaseRequest p")
    double sumCostoEstimado();
}
