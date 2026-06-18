package com.industrial.safety.course_service.unit.client;

import com.industrial.safety.course_service.client.OrderClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("OrderClient — Pruebas Unitarias")
class OrderClientTest {

    private static final String BASE = "http://order-service";

    private MockRestServiceServer server;
    private OrderClient orderClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        orderClient = new OrderClient(builder, BASE);
    }

    @Test
    @DisplayName("userOwnsCourse: true cuando hay una orden COMPLETED que incluye el curso")
    void ownsWhenCompletedOrderContainsCourse() {
        server.expect(requestTo(BASE + "/api/v1/orders/by-user/user-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"orderStatus\":\"COMPLETED\",\"orderLineItemsList\":[{\"idCurso\":\"course-1\"}]}]",
                        MediaType.APPLICATION_JSON));

        assertThat(orderClient.userOwnsCourse("user-1", "course-1")).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("userOwnsCourse: false cuando la orden no está COMPLETED")
    void notOwnedWhenOrderPending() {
        server.expect(requestTo(BASE + "/api/v1/orders/by-user/user-1"))
                .andRespond(withSuccess(
                        "[{\"orderStatus\":\"PENDING\",\"orderLineItemsList\":[{\"idCurso\":\"course-1\"}]}]",
                        MediaType.APPLICATION_JSON));

        assertThat(orderClient.userOwnsCourse("user-1", "course-1")).isFalse();
    }

    @Test
    @DisplayName("userOwnsCourse: false cuando el curso no aparece en ninguna orden")
    void notOwnedWhenCourseAbsent() {
        server.expect(requestTo(BASE + "/api/v1/orders/by-user/user-1"))
                .andRespond(withSuccess(
                        "[{\"orderStatus\":\"COMPLETED\",\"orderLineItemsList\":[{\"idCurso\":\"otro-curso\"}]}]",
                        MediaType.APPLICATION_JSON));

        assertThat(orderClient.userOwnsCourse("user-1", "course-1")).isFalse();
    }

    @Test
    @DisplayName("userOwnsCourse: false cuando el usuario no tiene órdenes")
    void notOwnedWhenNoOrders() {
        server.expect(requestTo(BASE + "/api/v1/orders/by-user/user-1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(orderClient.userOwnsCourse("user-1", "course-1")).isFalse();
    }

    @Test
    @DisplayName("userOwnsCourse: false (falla cerrado) ante error de order-service")
    void deniesOnError() {
        server.expect(requestTo(BASE + "/api/v1/orders/by-user/user-1"))
                .andRespond(withServerError());

        assertThat(orderClient.userOwnsCourse("user-1", "course-1")).isFalse();
    }
}
