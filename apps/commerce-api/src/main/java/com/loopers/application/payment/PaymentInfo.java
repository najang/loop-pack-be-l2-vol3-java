package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;

public record PaymentInfo(
    Long id,
    Long orderId,
    String pgTransactionId,
    int amount,
    String status,
    String cardType,
    String cardNo,
    String failureReason
) {
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
            payment.getId(),
            payment.getOrderId(),
            payment.getPgTransactionId(),
            payment.getAmount(),
            payment.getStatus().name(),
            payment.getCardType(),
            payment.getCardNo(),
            payment.getFailureReason()
        );
    }
}
