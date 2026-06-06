package com.logistica.purchase.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "epp.exchange";
    public static final String QUEUE = "epp.delivery.queue";
    public static final String ROUTING_KEY = "epp.delivered";

    // Exchange de plataforma (compartido) para eventos de solicitudes hacia solicitudes-service.
    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";
    public static final String SOLICITUD_SERVICIO_ROUTING_KEY = "event.solicitud.servicio";

    @Bean
    public DirectExchange eppExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange platformExchange() {
        return new TopicExchange(PLATFORM_EXCHANGE, true, false);
    }

    @Bean
    public Queue eppDeliveryQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding eppBinding() {
        return BindingBuilder.bind(eppDeliveryQueue()).to(eppExchange()).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
