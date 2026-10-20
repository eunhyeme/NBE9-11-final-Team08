package com.team08.backend.domain.commerce.payment.dto.nicepay;

public record NicepayPaymentErrorResponse(
        String resultCode,
        String resultMsg,
        String message
) {
}
