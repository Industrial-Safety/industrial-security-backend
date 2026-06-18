package com.industrial.safety.course_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Cliente HTTP hacia order-service para verificar la propiedad de un curso.
 * La URL base es configurable ({@code services.order.base-url}) para no mezclar
 * discovery: localhost/Eureka en local, DNS de Cloud Map en producción.
 */
@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(RestClient.Builder builder,
                       @Value("${services.order.base-url}") String orderBaseUrl) {
        this.restClient = builder.baseUrl(orderBaseUrl).build();
    }

    /**
     * Devuelve true si el usuario tiene una orden COMPLETED que incluye el curso.
     * Falla cerrado: ante cualquier error de comunicación devuelve false (deniega).
     */
    public boolean userOwnsCourse(String userId, String courseId) {
        OrderView[] orders;
        try {
            orders = restClient.get()
                    .uri("/api/v1/orders/by-user/{userId}", userId)
                    .retrieve()
                    .body(OrderView[].class);
        } catch (RestClientException ex) {
            return false;
        }
        if (orders == null) {
            return false;
        }
        for (OrderView order : orders) {
            if (!"COMPLETED".equalsIgnoreCase(order.orderStatus()) || order.orderLineItemsList() == null) {
                continue;
            }
            for (LineItem item : order.orderLineItemsList()) {
                if (courseId.equals(item.idCurso())) {
                    return true;
                }
            }
        }
        return false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderView(String orderStatus, List<LineItem> orderLineItemsList) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineItem(String idCurso) {
    }
}
