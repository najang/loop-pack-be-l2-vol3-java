package com.loopers.interfaces.consumer;

import com.loopers.infrastructure.order.OrderStreamerJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private OrderStreamerJpaRepository orderRepository;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @DisplayName("handlePaymentEvents() 시,")
    @Nested
    class HandlePaymentEvents {

        @DisplayName("COMPLETED 결제 이벤트를 수신하면 주문 상태를 ORDERED로 업데이트한다.")
        @Test
        void updatesOrderToOrdered_whenPaymentCompleted() {
            // arrange
            PaymentEventConsumer.PaymentEventMessage message =
                new PaymentEventConsumer.PaymentEventMessage("PAYMENT_RESULT", 1L, "COMPLETED");
            ConsumerRecord<String, PaymentEventConsumer.PaymentEventMessage> record =
                new ConsumerRecord<>("payment-events", 0, 0L, "1", message);
            Acknowledgment ack = mock(Acknowledgment.class);
            when(orderRepository.updateOrderStatus(1L, "ORDERED")).thenReturn(1);

            // act
            paymentEventConsumer.handlePaymentEvents(List.of(record), ack);

            // assert
            verify(orderRepository, times(1)).updateOrderStatus(1L, "ORDERED");
            verify(ack, times(1)).acknowledge();
        }

        @DisplayName("FAILED 결제 이벤트를 수신하면 주문 상태를 PAYMENT_FAILED로 업데이트한다.")
        @Test
        void updatesOrderToPaymentFailed_whenPaymentFailed() {
            // arrange
            PaymentEventConsumer.PaymentEventMessage message =
                new PaymentEventConsumer.PaymentEventMessage("PAYMENT_RESULT", 2L, "FAILED");
            ConsumerRecord<String, PaymentEventConsumer.PaymentEventMessage> record =
                new ConsumerRecord<>("payment-events", 0, 0L, "2", message);
            Acknowledgment ack = mock(Acknowledgment.class);
            when(orderRepository.updateOrderStatus(2L, "PAYMENT_FAILED")).thenReturn(1);

            // act
            paymentEventConsumer.handlePaymentEvents(List.of(record), ack);

            // assert
            verify(orderRepository, times(1)).updateOrderStatus(2L, "PAYMENT_FAILED");
            verify(ack, times(1)).acknowledge();
        }

        @DisplayName("null 메시지는 무시하고 ack한다.")
        @Test
        void skipsNull_andAcknowledges() {
            // arrange
            ConsumerRecord<String, PaymentEventConsumer.PaymentEventMessage> record =
                new ConsumerRecord<>("payment-events", 0, 0L, "1", null);
            Acknowledgment ack = mock(Acknowledgment.class);

            // act
            paymentEventConsumer.handlePaymentEvents(List.of(record), ack);

            // assert
            verify(orderRepository, never()).updateOrderStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            verify(ack, times(1)).acknowledge();
        }
    }
}
