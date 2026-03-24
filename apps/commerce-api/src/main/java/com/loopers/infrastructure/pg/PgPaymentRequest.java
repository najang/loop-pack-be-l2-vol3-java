package com.loopers.infrastructure.pg;

public record PgPaymentRequest(
    String orderId,
    String cardType,
    String cardNo,
    int amount,
    String callbackUrl
) {
}
