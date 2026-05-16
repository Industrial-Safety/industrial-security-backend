package com.logistica.purchase.repository;

import com.logistica.purchase.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
}