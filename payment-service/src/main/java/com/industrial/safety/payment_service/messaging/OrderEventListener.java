package com.industrial.safety.payment_service.messaging;

import com.industrial.safety.payment_service.config.RabbitMQConfig;
import com.industrial.safety.payment_service.dto.event.OrderCreatedEvent;
import com.industrial.safety.payment_service.exception.PaymentProcessingException;
import com.industrial.safety.payment_service.service.PaymentService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(@Payload OrderCreatedEvent event,
                               Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("Received OrderCreatedEvent order={} amount={} {}",
                event.orderNumber(), event.totalAmount(), event.currency());
        try {
            paymentService.processOrder(event);
            channel.basicAck(tag, false);
        } catch (PaymentProcessingException ex) {
            // Domain failures are already published as failure events; ack to remove from queue.
            log.warn("Domain failure handled gracefully for order {}: {}", event.orderNumber(), ex.getMessage());
            channel.basicAck(tag, false);
        } catch (RuntimeException ex) {
            log.error("Unrecoverable error processing order {} — sending to DLQ",
                    event.orderNumber(), ex);
            channel.basicNack(tag, false, false);
        }
    }
}
