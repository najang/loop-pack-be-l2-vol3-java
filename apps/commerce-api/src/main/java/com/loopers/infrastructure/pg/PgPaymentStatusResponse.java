package com.loopers.infrastructure.pg;

public record PgPaymentStatusResponse(
    String transactionKey,
    String status,
    String reason
) {
}
