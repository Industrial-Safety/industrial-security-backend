package com.logistica.purchase.dto;

import com.logistica.purchase.entity.PurchaseRequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull PurchaseRequestStatus estado) {}
