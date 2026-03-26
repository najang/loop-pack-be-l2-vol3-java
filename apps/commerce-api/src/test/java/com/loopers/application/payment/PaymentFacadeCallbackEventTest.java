package com.loopers.application.payment;

import com.loopers.application.order.OrderApplicationService;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxRepository;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.pg.PgCallbackRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeCallbackEventTest {

    @Mock
    private OrderApplicationService orderApplicationService;

    @Mock
    private PaymentApplicationService paymentApplicationService;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private PaymentFacade paymentFacade;

    @DisplayName("handleCallback() 시,")
    @Nested
    class HandleCallback {

        @DisplayName("SUCCESS 콜백이면 OutboxEvent(payment-events)를 저장하고, orderApplicationService를 직접 호출하지 않는다.")
        @Test
        void savesOutboxEvent_andDoesNotCallOrderService_onSuccess() {
            // arrange
            PgCallbackRequest callback = new PgCallbackRequest(1L, "tx-key", "SUCCESS", null);
            Payment payment = mock(Payment.class);
            when(payment.getOrderId()).thenReturn(10L);
            when(paymentApplicationService.handleCallback(callback)).thenReturn(payment);

            // act
            paymentFacade.handleCallback(callback);

            // assert
            verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
            verify(orderApplicationService, never()).confirmPayment(any());
            verify(orderApplicationService, never()).failPayment(any());
        }

        @DisplayName("FAILED 콜백이면 OutboxEvent(payment-events)를 저장하고, orderApplicationService를 직접 호출하지 않는다.")
        @Test
        void savesOutboxEvent_andDoesNotCallOrderService_onFailure() {
            // arrange
            PgCallbackRequest callback = new PgCallbackRequest(1L, "tx-key", "FAILED", "카드 한도 초과");
            Payment payment = mock(Payment.class);
            when(payment.getOrderId()).thenReturn(10L);
            when(paymentApplicationService.handleCallback(callback)).thenReturn(payment);

            // act
            paymentFacade.handleCallback(callback);

            // assert
            verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
            verify(orderApplicationService, never()).confirmPayment(any());
            verify(orderApplicationService, never()).failPayment(any());
        }
    }

    @DisplayName("syncWithPg() 시,")
    @Nested
    class SyncWithPg {

        @DisplayName("COMPLETED로 동기화되면 OutboxEvent를 저장하고, orderApplicationService를 직접 호출하지 않는다.")
        @Test
        void savesOutboxEvent_andDoesNotCallOrderService_onCompleted() {
            // arrange
            Long paymentId = 1L;
            Payment before = mock(Payment.class);
            when(before.getStatus()).thenReturn(PaymentStatus.PENDING);
            when(paymentApplicationService.findById(paymentId)).thenReturn(before);

            PaymentInfo updated = new PaymentInfo(paymentId, 10L, null, 1000, "COMPLETED", null, null, null);
            when(paymentApplicationService.syncWithPg(paymentId)).thenReturn(updated);

            // act
            paymentFacade.syncWithPg(paymentId);

            // assert
            verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
            verify(orderApplicationService, never()).confirmPayment(any());
            verify(orderApplicationService, never()).failPayment(any());
        }

        @DisplayName("PENDING이 아니면 OutboxEvent를 저장하지 않고 조기 반환한다.")
        @Test
        void returnsEarly_whenPaymentIsNotPending() {
            // arrange
            Long paymentId = 1L;
            Payment before = mock(Payment.class);
            when(before.getStatus()).thenReturn(PaymentStatus.COMPLETED);
            when(paymentApplicationService.findById(paymentId)).thenReturn(before);

            // act
            paymentFacade.syncWithPg(paymentId);

            // assert
            verify(outboxRepository, never()).save(any());
            verify(orderApplicationService, never()).confirmPayment(any());
        }
    }
}
