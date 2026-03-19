package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRecoverySchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentFacade paymentFacade;

    @InjectMocks
    private PaymentRecoveryScheduler paymentRecoveryScheduler;

    @DisplayName("recoverPendingPayments 실행 시,")
    @Nested
    class RecoverPendingPayments {

        @DisplayName("임계값보다 오래된 PENDING 결제 각각에 대해 syncWithPg를 호출한다.")
        @Test
        void callsSyncWithPg_forEachPendingPaymentOlderThanThreshold() {
            // arrange
            Payment payment1 = new Payment(1L, 10000, "SAMSUNG", "1234-5678-9012-3456");
            Payment payment2 = new Payment(2L, 20000, "VISA", "9876-5432-1098-7654");
            when(paymentRepository.findAllPendingOlderThan(any(ZonedDateTime.class)))
                .thenReturn(List.of(payment1, payment2));
            when(paymentFacade.syncWithPg(any())).thenReturn(null);

            // act
            paymentRecoveryScheduler.recoverPendingPayments();

            // assert
            verify(paymentFacade, times(2)).syncWithPg(any());
        }

        @DisplayName("개별 결제 동기화가 실패해도 다른 결제는 계속 처리된다.")
        @Test
        void skipsPayment_whenSyncThrowsException() {
            // arrange
            Payment payment1 = new Payment(1L, 10000, "SAMSUNG", "1234-5678-9012-3456");
            Payment payment2 = new Payment(2L, 20000, "VISA", "9876-5432-1098-7654");
            when(paymentRepository.findAllPendingOlderThan(any(ZonedDateTime.class)))
                .thenReturn(List.of(payment1, payment2));
            doThrow(new RuntimeException("PG 연결 실패"))
                .doReturn(null)
                .when(paymentFacade).syncWithPg(any());

            // act & assert (예외가 전파되지 않아야 함)
            paymentRecoveryScheduler.recoverPendingPayments();

            // assert
            verify(paymentFacade, times(2)).syncWithPg(any());
        }
    }
}
