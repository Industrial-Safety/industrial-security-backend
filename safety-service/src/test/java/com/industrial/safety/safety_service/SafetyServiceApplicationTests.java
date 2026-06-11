package com.industrial.safety.safety_service;

import com.industrial.safety.safety_service.integration.BaseSafetyIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false"
})
@ActiveProfiles("test")
class SafetyServiceApplicationTests extends BaseSafetyIT {

    @Test
    void contextLoads() {
    }

}
