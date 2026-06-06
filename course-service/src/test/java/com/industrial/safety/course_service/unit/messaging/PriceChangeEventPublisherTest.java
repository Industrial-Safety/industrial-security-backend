package com.industrial.safety.course_service.unit.messaging;

import com.industrial.safety.course_service.messaging.PriceChangeEventPublisher;
import com.industrial.safety.course_service.model.PriceChangeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceChangeEventPublisher — Pruebas Unitarias")
class PriceChangeEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks PriceChangeEventPublisher publisher;

    private PriceChangeRequest request(String reviewerComment) {
        return PriceChangeRequest.builder()
                .courseId("c1").courseTitle("Curso Seguridad")
                .currentPrice(50.0).requestedPrice(70.0).justification("inflación")
                .requesterId("u1").requesterName("Juan")
                .reviewerComment(reviewerComment)
                .build();
    }

    @Test
    @DisplayName("publishNewRequest: envía email a gerencia")
    void publishNewRequest_sends() {
        ReflectionTestUtils.setField(publisher, "managementEmail", "gerencia@x.com");
        publisher.publishNewRequest(request(null));
        then(rabbitTemplate).should().convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("publishApproved: con comentario de revisor")
    void publishApproved_withComment() {
        publisher.publishApproved(request("aprobado por demanda"));
        then(rabbitTemplate).should().convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("publishApproved: sin comentario de revisor")
    void publishApproved_noComment() {
        publisher.publishApproved(request(null));
        then(rabbitTemplate).should().convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("publishRejected: con y sin comentario")
    void publishRejected_bothBranches() {
        publisher.publishRejected(request("no procede"));
        publisher.publishRejected(request(null));
        then(rabbitTemplate).should(org.mockito.Mockito.times(2))
                .convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
