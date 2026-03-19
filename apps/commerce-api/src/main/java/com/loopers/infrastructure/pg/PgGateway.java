package com.loopers.infrastructure.pg;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PgGateway {

    private final PgSimulatorClient client;

    @CircuitBreaker(name = "pgCircuitBreaker", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgRetry")
    @Bulkhead(name = "pgBulkhead", type = Bulkhead.Type.SEMAPHORE)
    public PgPaymentResponse requestPayment(Long userId, PgPaymentRequest request) {
        return client.requestPayment(userId, request);
    }

    @CircuitBreaker(name = "pgCircuitBreaker", fallbackMethod = "inquirePaymentFallback")
    @Retry(name = "pgRetry")
    public PgPaymentStatusResponse inquirePayment(String transactionKey) {
        return client.inquirePayment(transactionKey);
    }

    private PgPaymentResponse requestPaymentFallback(Long userId, PgPaymentRequest request, Exception e) {
        throw new CoreException(ErrorType.PAYMENT_FAILED, "결제 서비스를 사용할 수 없습니다.");
    }

    private PgPaymentStatusResponse inquirePaymentFallback(String transactionKey, Exception e) {
        throw new CoreException(ErrorType.PAYMENT_FAILED, "결제 상태 조회 서비스를 사용할 수 없습니다.");
    }
}
