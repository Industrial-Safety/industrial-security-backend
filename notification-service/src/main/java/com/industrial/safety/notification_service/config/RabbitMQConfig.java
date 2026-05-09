package com.industrial.safety.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.swing.plaf.PanelUI;

@Configuration
public class RabbitMQConfig
{
    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String WS_ALERT_QUEUE = "notification.ws.alert.queue";

    @Bean
    public MessageConverter jsonMesssageConverter(){
        return new JacksonJsonMessageConverter();
    }
    @Bean
    public TopicExchange plataformExchange(){
        return new TopicExchange(PLATFORM_EXCHANGE);
    }
    @Bean
    public Queue emailQueue(){
        return new Queue(EMAIL_QUEUE,true);
    }
    @Bean
    public Queue wsAlertQueue(){
        return new Queue(WS_ALERT_QUEUE,true);
    }
    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange plataformExchange){
        return BindingBuilder.bind(emailQueue).to(plataformExchange).with("event.email.#");
    }
    @Bean
    public Binding wsAlertBinding(Queue wsAlertQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(wsAlertQueue).to(platformExchange).with("event.alert.#");
    }
}
