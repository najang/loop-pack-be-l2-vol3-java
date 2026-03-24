package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.infrastructure.pg.PgGateway;
import com.loopers.infrastructure.pg.PgPaymentStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    private static final Long PAYMENT_ID = 1L;
    private static final Long ORDER_ID = 10L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PgGateway pgGateway;

    @InjectMocks
    private PaymentApplicationService paymentApplicationService;

    @DisplayName("syncWithPg 호출 시,")
    @Nested
    class SyncWithPg {

        @DisplayName("PENDING이 아닌 결제는 PG를 조회하지 않고 현재 상태를 반환한다.")
        @Test
        void returnsCurrentInfo_withoutCallingPg_whenPaymentIsNotPending() {
            // arrange
            Payment payment = new Payment(ORDER_ID, 10000, "SAMSUNG", "1234-5678-9012-3456");
            payment.complete("TX-001");
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            // act
            PaymentInfo result = paymentApplicationService.syncWithPg(PAYMENT_ID);

            // assert
            assertThat(result.status()).isEqualTo("COMPLETED");
            verify(pgGateway, never()).inquirePayment(any());
        }

        @DisplayName("PG가 SUCCESS를 반환하면 결제가 완료 처리된다.")
        @Test
        void completesPayment_whenPgReportsSuccess() {
            // arrange
            Payment payment = new Payment(ORDER_ID, 10000, "SAMSUNG", "1234-5678-9012-3456");
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(pgGateway.inquirePayment(any()))
                .thenReturn(new PgPaymentStatusResponse("TX-001", "SUCCESS", null));
            when(paymentRepository.save(payment)).thenReturn(payment);

            // act
            PaymentInfo result = paymentApplicationService.syncWithPg(PAYMENT_ID);

            // assert
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.pgTransactionId()).isEqualTo("TX-001");
        }

        @DisplayName("PG가 FAILED를 반환하면 결제가 실패 처리된다.")
        @Test
        void failsPayment_whenPgReportsFailed() {
            // arrange
            Payment payment = new Payment(ORDER_ID, 10000, "SAMSUNG", "1234-5678-9012-3456");
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(pgGateway.inquirePayment(any()))
                .thenReturn(new PgPaymentStatusResponse(null, "FAILED", "카드 한도 초과"));
            when(paymentRepository.save(payment)).thenReturn(payment);

            // act
            PaymentInfo result = paymentApplicationService.syncWithPg(PAYMENT_ID);

            // assert
            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(result.failureReason()).isEqualTo("카드 한도 초과");
        }

        @DisplayName("PG가 PENDING을 반환하면 결제 상태가 변경되지 않고 저장도 되지 않는다.")
        @Test
        void leavesPaymentPending_whenPgAlsoReportsPending() {
            // arrange
            Payment payment = new Payment(ORDER_ID, 10000, "SAMSUNG", "1234-5678-9012-3456");
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(pgGateway.inquirePayment(any()))
                .thenReturn(new PgPaymentStatusResponse(null, "PENDING", null));

            // act
            PaymentInfo result = paymentApplicationService.syncWithPg(PAYMENT_ID);

            // assert
            assertThat(result.status()).isEqualTo("PENDING");
            verify(paymentRepository, never()).save(any());
        }
    }
}
