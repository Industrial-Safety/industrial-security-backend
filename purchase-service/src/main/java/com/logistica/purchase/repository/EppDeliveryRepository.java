package com.logistica.purchase.repository;

import com.logistica.purchase.entity.EppDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EppDeliveryRepository extends JpaRepository<EppDelivery, Long> {
    List<EppDelivery> findByWorkerDni(String workerDni);
}
