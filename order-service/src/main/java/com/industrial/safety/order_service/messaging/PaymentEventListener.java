package com.industrial.safety.order_service.messaging;

import com.industrial.safety.order_service.config.RabbitMQConfig;
import com.industrial.safety.order_service.dto.event.PaymentResultEvent;
import com.industrial.safety.order_service.exception.ResourceNotFoundException;
import com.industrial.safety.order_service.service.OrderService;
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
public class PaymentEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_RESULT_QUEUE)
    public void onPaymentResult(@Payload PaymentResultEvent event,
                                Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("Received PaymentResultEvent order={} success={}",
                event.orderNumber(), event.success());
        try {
            orderService.processPaymentResult(event);
            channel.basicAck(tag, false);
        } catch (ResourceNotFoundException ex) {
            // Order vanished — nothing we can do; ack to drop instead of poison-looping.
            log.error("Discarding payment result for unknown order {}", event.orderNumber(), ex);
            channel.basicAck(tag, false);
        } catch (RuntimeException ex) {
            log.error("Unrecoverable failure processing payment result for order {} — DLQ",
                    event.orderNumber(), ex);
            channel.basicNack(tag, false, false);
        }
    }
}
