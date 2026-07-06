package com.industrial.safety.eventos.config;

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
 * - PUBLICA el evento clasificado como ERROR/CRITICAL hacia el exchange de la plataforma,
 *   para que incidencias-service lo consuma y genere la incidencia correspondiente.
 * - CONSUME la confirmación de vuelta (incidencia creada) para enlazar el código de la
 *   incidencia al evento en el tablero.
 * Reusa el mismo exchange topico que ya declaran los demas servicios.
 */
@Configuration
public class RabbitMQConfig {

    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";

    /** Un evento (ERROR/CRITICAL) que debe convertirse en incidencia. */
    public static final String RK_INCIDENCIA_DESDE_EVENTO = "event.incidencia.desde-evento";

    /** Confirmación de incidencias-service: la incidencia del evento ya existe. */
    public static final String INCIDENCIA_CREADA_QUEUE = "eventos.incidencia-creada.queue";
    public static final String RK_INCIDENCIA_CREADA = "event.evento.incidencia-creada";

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    TopicExchange platformExchange() {
        return new TopicExchange(PLATFORM_EXCHANGE, true, false);
    }

    @Bean
    Queue incidenciaCreadaQueue() {
        return QueueBuilder.durable(INCIDENCIA_CREADA_QUEUE).build();
    }

    @Bean
    Binding incidenciaCreadaBinding(Queue incidenciaCreadaQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(incidenciaCreadaQueue).to(platformExchange).with(RK_INCIDENCIA_CREADA);
    }
}
