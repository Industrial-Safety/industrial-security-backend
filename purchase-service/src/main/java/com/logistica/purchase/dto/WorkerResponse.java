package com.logistica.purchase.dto;

public record WorkerResponse(
        String id,
        String dni,
        String name,
        String lastName,
        String role
) {}
