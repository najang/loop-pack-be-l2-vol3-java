package com.loopers.infrastructure.pg;

public record PgPaymentStatusResponse(
    Long paymentId,
    String pgTransactionId,
    String status,
    String failureReason
) {
}
