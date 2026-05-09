package com.industrial.safety.order_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";
    public static final String DLX_EXCHANGE = "industrial.safety.dlx";

    public static final String PAYMENT_RESULT_QUEUE = "order.payment.result.queue";
    public static final String PAYMENT_RESULT_DLQ = "order.payment.result.dlq";

    public static final String ORDER_CREATED_ROUTING_KEY = "event.order.created";
    public static final String EMAIL_PURCHASE_SUCCESS_KEY = "event.email.purchase.success";
    public static final String EMAIL_PURCHASE_FAILED_KEY = "event.email.purchase.failed";
    public static final String ALERT_PURCHASE_SUCCESS_KEY = "event.alert.purchase.success";
    public static final String ALERT_PURCHASE_FAILED_KEY = "event.alert.purchase.failed";

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
    Queue paymentResultQueue() {
        return QueueBuilder.durable(PAYMENT_RESULT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_RESULT_DLQ)
                .build();
    }

    @Bean
    Queue paymentResultDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_RESULT_DLQ).build();
    }

    @Bean
    Binding paymentResultBinding(Queue paymentResultQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(paymentResultQueue).to(platformExchange).with("event.payment.#");
    }

    @Bean
    Binding paymentResultDlqBinding(Queue paymentResultDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentResultDeadLetterQueue)
                .to(deadLetterExchange).with(PAYMENT_RESULT_DLQ);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        return template;
    }
}
