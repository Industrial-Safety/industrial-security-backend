package com.industrial.safety.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PLATFORM_EXCHANGE = "industrial.safety.topic";
    public static final String DLX_EXCHANGE = "industrial.safety.dlx";

    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String EMAIL_DLQ = "notification.email.dlq";

    public static final String WS_ALERT_QUEUE = "notification.ws.alert.queue";
    public static final String WS_ALERT_DLQ = "notification.ws.alert.dlq";

    public static final String CERT_QUEUE = "notification.certificate.queue";
    public static final String CERT_DLQ   = "notification.certificate.dlq";

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
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .build();
    }

    @Bean
    Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    Queue wsAlertQueue() {
        return QueueBuilder.durable(WS_ALERT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", WS_ALERT_DLQ)
                .build();
    }

    @Bean
    Queue wsAlertDeadLetterQueue() {
        return QueueBuilder.durable(WS_ALERT_DLQ).build();
    }

    @Bean
    Binding emailBinding(Queue emailQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(emailQueue).to(platformExchange).with("event.email.#");
    }

    @Bean
    Binding wsAlertBinding(Queue wsAlertQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(wsAlertQueue).to(platformExchange).with("event.alert.#");
    }

    @Bean
    Binding emailDlqBinding(Queue emailDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDeadLetterQueue).to(deadLetterExchange).with(EMAIL_DLQ);
    }

    @Bean
    Binding wsAlertDlqBinding(Queue wsAlertDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(wsAlertDeadLetterQueue).to(deadLetterExchange).with(WS_ALERT_DLQ);
    }

    @Bean
    Queue certQueue() {
        return QueueBuilder.durable(CERT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CERT_DLQ)
                .build();
    }

    @Bean
    Queue certDeadLetterQueue() {
        return QueueBuilder.durable(CERT_DLQ).build();
    }

    @Bean
    Binding certBinding(Queue certQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(certQueue).to(platformExchange).with("event.certificate.#");
    }

    @Bean
    Binding certDlqBinding(Queue certDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(certDeadLetterQueue).to(deadLetterExchange).with(CERT_DLQ);
    }
}
