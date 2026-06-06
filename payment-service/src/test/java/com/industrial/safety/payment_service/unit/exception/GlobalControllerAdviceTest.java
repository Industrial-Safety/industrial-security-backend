package com.industrial.safety.payment_service.unit.exception;

import com.industrial.safety.payment_service.exception.GlobalControllerAdvice;
import com.industrial.safety.payment_service.exception.InvalidWebhookSignatureException;
import com.industrial.safety.payment_service.exception.PaymentNotFoundException;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("GlobalControllerAdvice (payment) — Pruebas Unitarias")
class GlobalControllerAdviceTest {

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice();

    @Test
    @DisplayName("PaymentNotFoundException -> 404")
    void paymentNotFound_returns404() {
        ProblemDetail pd = advice.handlePaymentNotFound(
                new PaymentNotFoundException("ORD-1"), mock(WebRequest.class));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("PaymentProcessingException -> 422")
    void paymentProcessing_returns422() {
        ProblemDetail pd = advice.handlePaymentProcessing(
                new PaymentProcessingException("code", "msg"), mock(WebRequest.class));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @Test
    @DisplayName("InvalidWebhookSignatureException -> 401")
    void invalidSignature_returns401() {
        ProblemDetail pd = advice.handleInvalidSignature(new InvalidWebhookSignatureException("bad sig"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400")
    void validation_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        given(ex.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldErrors())
                .willReturn(List.of(new FieldError("obj", "field", "msg")));
        ProblemDetail pd = advice.handleValidation(ex);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Exception genérica -> 500")
    void generic_returns500() {
        ProblemDetail pd = advice.handleGeneric(new Exception("boom"), mock(WebRequest.class));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
