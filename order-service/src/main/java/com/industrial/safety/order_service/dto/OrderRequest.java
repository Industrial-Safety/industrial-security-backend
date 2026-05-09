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

    /** Card token returned by MercadoPago Payment Brick onSubmit (ephemeral, single-use). */
    @NotBlank
    private String mpToken;

    /** e.g. "visa", "master", "amex" — also from the Brick payload. */
    @NotBlank
    private String mpPaymentMethodId;

    /** 1 by default; the Brick exposes installment options and returns the chosen value. */
    private Integer mpInstallments;

    /** Optional issuer id supplied by the Brick when relevant. */
    private String mpIssuerId;

    /** Buyer email captured by the Brick (may differ from session email). */
    @Email
    private String mpPayerEmail;

    /** Buyer doc type (DNI, CE, RUC, etc.) captured by the Brick. */
    private String mpPayerIdType;

    /** Buyer doc number captured by the Brick. */
    private String mpPayerIdNumber;

    @NotEmpty
    @Valid
    private List<OrderLineItemsRequest> orderLineItemsList;
}
