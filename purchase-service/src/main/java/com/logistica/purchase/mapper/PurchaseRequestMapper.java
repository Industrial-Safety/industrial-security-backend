package com.logistica.purchase.mapper;

import com.logistica.purchase.dto.PurchaseRequestCreateRequest;
import com.logistica.purchase.dto.PurchaseRequestResponse;
import com.logistica.purchase.entity.PurchaseRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PurchaseRequestMapper {
    PurchaseRequest toEntity(PurchaseRequestCreateRequest request);
    PurchaseRequestResponse toResponse(PurchaseRequest entity);
}
