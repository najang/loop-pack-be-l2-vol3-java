package com.loopers.infrastructure.pg;

public record PgPaymentRequest(
    Long paymentId,
    Long orderId,
    String cardType,
    String cardNo,
    int amount,
    String callbackUrl
) {
}
