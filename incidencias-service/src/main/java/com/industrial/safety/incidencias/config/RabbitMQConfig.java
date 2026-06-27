package com.industrial.safety.incidencias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * - PUBLICA notificaciones al reportero reusando las colas de notification-service
 *   (routing keys "event.alert.#").
 * - DECLARA y CONSUME la cola de sincronización con Freshservice, con DLQ:
 *   si la sincronización falla tras los reintentos, el mensaje cae a la DLQ
 *   (ninguna incidencia se pierde).
 */
@Configuration
public class RabbitMQConfig {

    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";
    public static final String DLX_EXCHANGE = "industrial.safety.dlx";

    /** Notificaciones al reportero (las consume notification-service). */
    public static final String RK_ALERT_CREADA = "event.alert.incidencia.creada";
    public static final String RK_ALERT_RESUELTA = "event.alert.incidencia.resuelta";

    /** Sincronización con Freshservice (la consume este servicio). */
    public static final String SYNC_QUEUE = "incidencias.sync.queue";
    public static final String SYNC_DLQ = "incidencias.sync.dlq";
    public static final String RK_SYNC = "event.incidencia.sync";

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    TopicExchange platformExchange() {
        return new TopicExchange(PLATFORM_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    Queue syncQueue() {
        return QueueBuilder.durable(SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SYNC_DLQ)
                .build();
    }

    @Bean
    Queue syncDeadLetterQueue() {
        return QueueBuilder.durable(SYNC_DLQ).build();
    }

    @Bean
    Binding syncBinding(Queue syncQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(syncQueue).to(platformExchange).with(RK_SYNC);
    }

    @Bean
    Binding syncDlqBinding(Queue syncDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(syncDeadLetterQueue).to(deadLetterExchange).with(SYNC_DLQ);
    }
}
