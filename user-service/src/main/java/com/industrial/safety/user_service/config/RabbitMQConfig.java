package com.industrial.safety.user_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración mínima de AMQP para PUBLICAR solicitudes de ACCESO al
 * exchange de plataforma. user-service solo publica (no consume colas);
 * el solicitudes-service ya declara la cola y el binding "event.solicitud.#".
 */
@Configuration
public class RabbitMQConfig {

    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";
    public static final String ACCESO_ROUTING_KEY = "event.solicitud.acceso";

    @Bean
    public TopicExchange platformExchange() {
        return new TopicExchange(PLATFORM_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
