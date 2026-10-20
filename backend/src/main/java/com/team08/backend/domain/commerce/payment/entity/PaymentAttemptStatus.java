package com.team08.backend.domain.commerce.payment.entity;

public enum PaymentAttemptStatus {
    REQUESTED,
    SUCCESS,
    DECLINED,
    PROVIDER_ERROR,
    TIMEOUT,
    UNKNOWN
}
