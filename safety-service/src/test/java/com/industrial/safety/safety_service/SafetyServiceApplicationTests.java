package com.industrial.safety.safety_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false"
})
@ActiveProfiles("test")
class SafetyServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
