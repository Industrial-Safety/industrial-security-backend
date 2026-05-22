package com.logistica.purchase.messaging;

import com.logistica.purchase.config.RabbitMQConfig;
import com.logistica.purchase.dto.EppDeliveredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EppEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishDelivery(EppDeliveredEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            log.info("Evento EPP publicado: dni={} item={}", event.workerDni(), event.inventoryItemId());
        } catch (Exception e) {
            log.warn("No se pudo publicar evento EPP a RabbitMQ: {}", e.getMessage());
        }
    }
}
