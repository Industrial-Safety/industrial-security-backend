package com.industrial.safety.course_service.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;

public abstract class BaseCourseIT {

    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    static {
        mongo.start();
    }
}
