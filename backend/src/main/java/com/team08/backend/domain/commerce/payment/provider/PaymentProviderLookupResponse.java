package com.team08.backend.domain.commerce.payment.provider;

import java.time.OffsetDateTime;

public record PaymentProviderLookupResponse(
        String paymentKey,
        String orderId,
        String status,
        String method,
        long totalAmount,
        OffsetDateTime approvedAt
) {
}
