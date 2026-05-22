package com.industrial.safety.notification_service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.industrial.safety.notification_service.service.EmailService;

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
class NotificationServiceApplicationTests {

	@MockitoBean
	EmailService emailService;

	@Test
	void contextLoads() {
	}

}
