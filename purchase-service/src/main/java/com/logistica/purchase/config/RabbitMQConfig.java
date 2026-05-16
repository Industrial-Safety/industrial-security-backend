package com.logistica.purchase.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "epp.exchange";
    public static final String QUEUE = "epp.delivery.queue";
    public static final String ROUTING_KEY = "epp.delivered";

    @Bean
    public DirectExchange eppExchange() {
        return new DirectExchange(EXCHANGE);
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
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
