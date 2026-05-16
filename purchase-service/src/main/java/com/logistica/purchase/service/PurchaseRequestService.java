package com.logistica.purchase.service;

import com.logistica.purchase.dto.PurchaseRequestCreateRequest;
import com.logistica.purchase.dto.PurchaseRequestResponse;
import com.logistica.purchase.dto.StatsResponse;

import java.util.List;

public interface PurchaseRequestService {
    List<PurchaseRequestResponse> getAll();
    PurchaseRequestResponse getById(Long id);
    PurchaseRequestResponse create(PurchaseRequestCreateRequest request);
    PurchaseRequestResponse updateStatus(Long id, String estado);
    StatsResponse getStats();
    List<PurchaseRequestResponse> getApproved();
}
