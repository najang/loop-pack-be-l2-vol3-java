package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentFacade paymentFacade;

    @Scheduled(fixedDelay = 60000)
    public void recoverPendingPayments() {
        ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(5);
        List<Payment> targets = paymentRepository.findAllPendingOlderThan(threshold);
        for (Payment payment : targets) {
            try {
                paymentFacade.syncWithPg(payment.getId());
            } catch (Exception e) {
                log.warn("결제 상태 복구 실패: paymentId={}", payment.getId(), e);
            }
        }
    }
}
