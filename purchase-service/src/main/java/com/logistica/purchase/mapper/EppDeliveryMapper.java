package com.logistica.purchase.mapper;

import com.logistica.purchase.dto.EppDeliveryResponse;
import com.logistica.purchase.entity.EppDelivery;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EppDeliveryMapper {
    EppDeliveryResponse toResponse(EppDelivery entity);
}
