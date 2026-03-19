package com.loopers.application.payment;

import com.loopers.application.order.OrderApplicationService;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.pg.PgCallbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private final OrderApplicationService orderApplicationService;
    private final PaymentApplicationService paymentApplicationService;
    private final PlatformTransactionManager transactionManager;

    public OrderInfo createOrderAndPay(Long userId, Long productId, int quantity, Long userCouponId, String cardType, String cardNo) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        record OrderAndPayment(Order order, Payment payment) {}

        OrderAndPayment pair = Objects.requireNonNull(transactionTemplate.execute(status -> {
            Order order = orderApplicationService.create(userId, productId, quantity, userCouponId);
            Payment payment = paymentApplicationService.create(order.getId(), order.getFinalTotalPrice(), cardType, cardNo);
            return new OrderAndPayment(order, payment);
        }));

        paymentApplicationService.requestToPg(pair.payment(), userId);

        return OrderInfo.from(pair.order());
    }

    @Transactional
    public void handleCallback(PgCallbackRequest callback) {
        Payment payment = paymentApplicationService.handleCallback(callback);

        if ("SUCCESS".equals(callback.status())) {
            orderApplicationService.confirmPayment(payment.getOrderId());
        } else {
            orderApplicationService.failPayment(payment.getOrderId());
        }
    }

    @Transactional
    public PaymentInfo syncWithPg(Long paymentId) {
        Payment before = paymentApplicationService.findById(paymentId);
        if (before.getStatus() != PaymentStatus.PENDING) {
            return PaymentInfo.from(before);
        }

        PaymentInfo updated = paymentApplicationService.syncWithPg(paymentId);

        if ("COMPLETED".equals(updated.status())) {
            orderApplicationService.confirmPayment(updated.orderId());
        } else if ("FAILED".equals(updated.status())) {
            orderApplicationService.failPayment(updated.orderId());
        }

        return updated;
    }
}
