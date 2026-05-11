package com.industrial.safety.notification_service.listener;

import com.industrial.safety.notification_service.config.RabbitMQConfig;
import com.industrial.safety.notification_service.dto.CertificateEmailRequest;
import com.industrial.safety.notification_service.dto.EmailNotificationRequest;
import com.industrial.safety.notification_service.dto.WebAlertRequest;
import com.industrial.safety.notification_service.service.EmailService;
import com.industrial.safety.notification_service.service.WebAlertService;
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
public class NotificationEventConsumer {

    private final EmailService emailService;
    private final WebAlertService webAlertService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmailEvent(@Payload EmailNotificationRequest request,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                  @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey)
            throws IOException {
        boolean success = routingKey == null || !routingKey.contains("failed");
        log.info("[email-event] to={} subject='{}' success={}", request.to(), request.subject(), success);
        try {
            emailService.sendPurchaseEmail(request, success);
            channel.basicAck(tag, false);
        } catch (RuntimeException ex) {
            log.error("[email-event] Unexpected error — DLQ", ex);
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.CERT_QUEUE)
    public void consumeCertificateEvent(@Payload CertificateEmailRequest request,
                                        Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws IOException {
        log.info("[cert-event] to={} course='{}'", request.to(), request.courseName());
        try {
            emailService.sendCertificateEmail(request);
            channel.basicAck(tag, false);
        } catch (RuntimeException ex) {
            log.error("[cert-event] Unexpected error — DLQ", ex);
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.WS_ALERT_QUEUE)
    public void consumeWebAlertEvent(@Payload WebAlertRequest alert,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                     @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey)
            throws IOException {
        boolean success = routingKey == null || !routingKey.contains("failed");
        log.info("[ws-event] target={} title='{}' success={}", alert.targetUserId(), alert.title(), success);
        try {
            webAlertService.pushAlert(alert, success);
            channel.basicAck(tag, false);
        } catch (RuntimeException ex) {
            log.error("[ws-event] Unexpected error — DLQ", ex);
            channel.basicNack(tag, false, false);
        }
    }
}
