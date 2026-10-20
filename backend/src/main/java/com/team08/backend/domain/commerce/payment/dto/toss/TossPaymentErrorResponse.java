package com.team08.backend.domain.commerce.payment.dto.toss;

public record TossPaymentErrorResponse(
        String code,
        String message
) {
}
