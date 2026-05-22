package com.industrial.safety.order_service.dto.event;

import java.math.BigDecimal;

public record OrderItemEvent(
        String courseId,
        String courseName,
        BigDecimal price
) {
}
