package com.industrial.safety.notification_service.listener;

import com.industrial.safety.notification_service.config.RabbitMQConfig;
import com.industrial.safety.notification_service.dto.EmailNotificationRequest;
import com.industrial.safety.notification_service.dto.WebAlertRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmailEvent(EmailNotificationRequest request) {
        System.out.println("📩 [NUEVO EVENTO] Preparando envío de correo...");
        System.out.println("Destinatario: " + request.to());
        System.out.println("Curso: " + request.courseName());
        System.out.println("URL del Recibo: " + request.pdfReceiptUrl());
    }

    // Escucha la cola de alertas WebSockets
    @RabbitListener(queues = RabbitMQConfig.WS_ALERT_QUEUE)
    public void consumeWebAlertEvent(WebAlertRequest alert) {
        System.out.println("🔔 [NUEVA ALERTA] Preparando notificación web...");
        System.out.println("Usuario destino ID: " + alert.targetUserId());
        System.out.println("Mensaje: " + alert.message());
    }
}