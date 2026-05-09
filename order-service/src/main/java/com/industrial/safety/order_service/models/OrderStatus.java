package com.industrial.safety.order_service.models;

public enum OrderStatus {
    PENDING,    // newly created, awaiting payment
    PROCESSING, // payment in flight
    COMPLETED,  // payment succeeded
    FAILED,     // payment failed
    CANCELLED   // user/admin cancelled
}
