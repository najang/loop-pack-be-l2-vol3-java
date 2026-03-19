package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.pg.PgCallbackRequest;
import com.loopers.infrastructure.pg.PgGateway;
import com.loopers.infrastructure.pg.PgPaymentRequest;
import com.loopers.infrastructure.pg.PgPaymentStatusResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PgGateway pgGateway;

    @Value("${pg.simulator.callback-base-url}")
    private String callbackBaseUrl;

    @Transactional
    public Payment create(Long orderId, int amount, String cardType, String cardNo) {
        Payment payment = new Payment(orderId, amount, cardType, cardNo);
        return paymentRepository.save(payment);
    }

    @Transactional
    public void requestToPg(Payment payment, Long userId) {
        PgPaymentRequest request = new PgPaymentRequest(
            Long.toString(payment.getOrderId()),
            payment.getCardType(),
            payment.getCardNo(),
            payment.getAmount(),
            callbackBaseUrl + "/" + payment.getId() + "/callback"
        );
        var response = pgGateway.requestPayment(userId, request);
        payment.storeTransactionKey(response.transactionKey());
        paymentRepository.save(payment);
    }

    @Transactional
    public Payment handleCallback(PgCallbackRequest callback) {
        Payment payment = paymentRepository.findById(callback.paymentId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if ("SUCCESS".equals(callback.status())) {
            payment.complete(callback.transactionKey());
        } else {
            payment.fail(callback.failureReason());
        }
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Payment findById(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public PaymentInfo syncWithPg(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return PaymentInfo.from(payment);
        }

        PgPaymentStatusResponse pg = pgGateway.inquirePayment(payment.getPgTransactionId());

        if ("SUCCESS".equals(pg.status())) {
            payment.complete(pg.transactionKey());
            paymentRepository.save(payment);
        } else if ("FAILED".equals(pg.status())) {
            payment.fail(pg.reason());
            paymentRepository.save(payment);
        }

        return PaymentInfo.from(payment);
    }
}
