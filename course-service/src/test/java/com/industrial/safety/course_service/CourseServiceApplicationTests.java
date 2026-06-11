package com.industrial.safety.course_service;

import com.industrial.safety.course_service.integration.BaseCourseIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false"
})
@ActiveProfiles("test")
@Tag("integration")
class CourseServiceApplicationTests extends BaseCourseIT {

    @Test
    void contextLoads() {
    }
}
