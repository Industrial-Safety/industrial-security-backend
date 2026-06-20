package com.industrial.safety.solicitudes.config;

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
 * Consume los eventos de solicitud publicados por los 9 microservicios
 * en el exchange de plataforma. Cualquier routing key "event.solicitud.*"
 * (servicio / informacion / acceso) cae en una unica cola central.
 */
@Configuration
public class RabbitMQConfig {

    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";
    public static final String DLX_EXCHANGE = "industrial.safety.dlx";

    public static final String SOLICITUDES_QUEUE = "solicitudes.jira.queue";
    public static final String SOLICITUDES_DLQ = "solicitudes.jira.dlq";

    /** Patron de routing: event.solicitud.servicio / .informacion / .acceso */
    public static final String ROUTING_PATTERN = "event.solicitud.#";

    /** Cola y patron para resoluciones (aprobada/rechazada) → transicion del ticket Jira. */
    public static final String RESOLUCION_QUEUE = "solicitudes.resolucion.queue";
    public static final String RESOLUCION_PATTERN = "event.resolucion.#";

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
    Queue solicitudesQueue() {
        return QueueBuilder.durable(SOLICITUDES_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SOLICITUDES_DLQ)
                .build();
    }

    @Bean
    Queue solicitudesDeadLetterQueue() {
        return QueueBuilder.durable(SOLICITUDES_DLQ).build();
    }

    @Bean
    Binding solicitudesBinding(Queue solicitudesQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(solicitudesQueue).to(platformExchange).with(ROUTING_PATTERN);
    }

    @Bean
    Binding solicitudesDlqBinding(Queue solicitudesDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(solicitudesDeadLetterQueue).to(deadLetterExchange).with(SOLICITUDES_DLQ);
    }

    @Bean
    Queue resolucionQueue() {
        return QueueBuilder.durable(RESOLUCION_QUEUE).build();
    }

    @Bean
    Binding resolucionBinding(Queue resolucionQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(resolucionQueue).to(platformExchange).with(RESOLUCION_PATTERN);
    }
}
