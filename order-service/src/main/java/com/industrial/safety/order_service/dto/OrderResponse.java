package com.industrial.safety.order_service.dto;

import com.industrial.safety.order_service.models.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private String userId;
    private String userEmail;
    private OrderStatus orderStatus;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentIntentId;
    private String receiptUrl;
    private String failureReason;
    private String couponCode;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private Instant createdAt;
    private Instant paidAt;
    private List<OrderLineItemsResponse> orderLineItemsList;
}
