package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentV1Dto {

    public record CallbackRequest(
        @NotNull Long orderId,
        @NotBlank String status,
        String failureReason
    ) {
    }

    public record PaymentResponse(
        Long id,
        Long orderId,
        String status,
        String pgTransactionId,
        String failureReason
    ) {
        public static PaymentResponse from(PaymentInfo info) {
            return new PaymentResponse(
                info.id(),
                info.orderId(),
                info.status(),
                info.pgTransactionId(),
                info.failureReason()
            );
        }
    }
}
