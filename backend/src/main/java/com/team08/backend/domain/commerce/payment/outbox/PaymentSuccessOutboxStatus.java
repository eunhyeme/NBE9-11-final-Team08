package com.team08.backend.domain.commerce.payment.outbox;

public enum PaymentSuccessOutboxStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    DEAD
}
