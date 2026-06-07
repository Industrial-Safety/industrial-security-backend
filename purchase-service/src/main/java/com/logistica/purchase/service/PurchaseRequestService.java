package com.logistica.purchase.service;

import com.logistica.purchase.dto.PurchaseRequestCreateRequest;
import com.logistica.purchase.dto.PurchaseRequestResponse;
import com.logistica.purchase.dto.StatsResponse;
import com.logistica.purchase.entity.PurchaseRequestStatus;

import java.util.List;

public interface PurchaseRequestService {
    List<PurchaseRequestResponse> getAll();
    PurchaseRequestResponse getById(Long id);
    PurchaseRequestResponse create(PurchaseRequestCreateRequest request);
    PurchaseRequestResponse updateStatus(Long id, PurchaseRequestStatus estado);
    StatsResponse getStats();
    List<PurchaseRequestResponse> getApproved();
}
