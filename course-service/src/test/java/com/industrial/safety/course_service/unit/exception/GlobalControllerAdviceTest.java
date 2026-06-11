package com.industrial.safety.course_service.unit.exception;

import com.industrial.safety.course_service.exception.GlobalControllerAdvice;
import com.industrial.safety.course_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalControllerAdvice — Pruebas Unitarias")
class GlobalControllerAdviceTest {

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice();
    private final ServletWebRequest      webRequest =
            new ServletWebRequest(new MockHttpServletRequest("GET", "/api/test"));

    @Test
    @DisplayName("handleResourceNotFoundException: retorna 404 con metadatos del recurso")
    void handleResourceNotFoundException_returns404WithDetail() {
        var ex = new ResourceNotFoundException("Course", "id", "uuid-99");

        ProblemDetail pd = advice.handleResourceNotFoundException(ex, webRequest);

        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getProperties()).containsKey("Resource");
        assertThat(pd.getProperties()).containsKey("Field");
        assertThat(pd.getProperties()).containsKey("Value");
    }

    @Test
    @DisplayName("handelException: retorna 500 para excepciones inesperadas")
    void handelException_returns500ForGenericException() {
        var ex = new RuntimeException("fallo inesperado en producción");

        ProblemDetail pd = advice.handelException(ex, webRequest);

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getTitle()).isEqualTo("Interal Server Error");
    }

    @Test
    @DisplayName("handelException: incluye Timestamp en la respuesta")
    void handelException_includesTimestamp() {
        ProblemDetail pd = advice.handelException(new Exception("err"), webRequest);

        assertThat(pd.getProperties()).containsKey("Timestap");
    }
}
