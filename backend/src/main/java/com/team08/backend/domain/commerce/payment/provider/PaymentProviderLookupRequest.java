package com.team08.backend.domain.commerce.payment.provider;

public record PaymentProviderLookupRequest(
        String paymentKey,
        String orderId
) {
}
