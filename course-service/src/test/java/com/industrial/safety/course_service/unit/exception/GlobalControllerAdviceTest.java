package com.industrial.safety.course_service.unit.exception;

import com.industrial.safety.course_service.exception.GlobalControllerAdvice;
import com.industrial.safety.course_service.exception.ResourceNotFoundException;
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

@DisplayName("GlobalControllerAdvice (course) — Pruebas Unitarias")
class GlobalControllerAdviceTest {

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice();

    @Test
    @DisplayName("ResourceNotFoundException -> 404")
    void resourceNotFound_returns404() {
        WebRequest request = mock(WebRequest.class);
        ProblemDetail pd = advice.handleResourceNotFoundException(
                new ResourceNotFoundException("Course", "id", "c1"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400")
    void methodArgumentNotValid_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        given(ex.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldErrors())
                .willReturn(List.of(new FieldError("courseRequest", "title", "must not be blank")));
        ProblemDetail pd = advice.handleMethodArgumentNotValidException(ex);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Exception genérica -> 500")
    void generic_returns500() {
        WebRequest request = mock(WebRequest.class);
        ProblemDetail pd = advice.handelException(new Exception("boom"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
