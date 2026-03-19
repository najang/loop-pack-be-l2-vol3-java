package com.loopers.infrastructure.pg;

public record PgCallbackRequest(
    Long paymentId,
    String transactionKey,
    String status,
    String failureReason
) {
}
