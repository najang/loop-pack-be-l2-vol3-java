package com.loopers.pg;

public record PaymentRequest(
    Long paymentId,
    Long orderId,
    String cardType,
    String cardNo,
    int amount,
    String callbackUrl
) {
}
