package com.industrial.safety.notification_service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class NotificationServiceApplicationTests {

	@Container
	static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.13-management");

	@DynamicPropertySource
	static void setRabbitProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
		registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
		registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
		registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
	}

	@Test
	void contextLoads() {
	}

}
