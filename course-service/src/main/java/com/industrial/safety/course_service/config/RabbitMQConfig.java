package com.industrial.safety.course_service.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Shared exchange — declared by notification-service, course-service just publishes
    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";

    // Routing keys — reuse notification-service's existing queue bindings
    public static final String EMAIL_PRICE_REQUEST_KEY  = "event.email.price.request";
    public static final String ALERT_PRICE_APPROVED_KEY = "event.alert.price.approved";
    public static final String ALERT_PRICE_REJECTED_KEY = "event.alert.price.rejected";

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                   MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
