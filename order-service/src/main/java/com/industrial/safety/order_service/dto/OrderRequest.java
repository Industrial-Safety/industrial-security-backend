package com.industrial.safety.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    @NotBlank
    private String userId;

    @Email
    private String userEmail;

    private String currency;

    /** Token from Stripe.js / simulator (e.g., "pm_card_visa" or raw 4242 for the simulator). */
    private String paymentMethodToken;

    @NotEmpty
    @Valid
    private List<OrderLineItemsRequest> orderLineItemsList;
}
