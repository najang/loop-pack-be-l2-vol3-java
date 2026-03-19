package com.loopers.infrastructure.pg;

public record PgCallbackRequest(
    Long paymentId,
    Long orderId,
    String status,
    String failureReason
) {
}
