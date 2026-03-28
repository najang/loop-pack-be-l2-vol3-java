package com.loopers.interfaces.consumer;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.infrastructure.order.OrderStreamerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventConsumer {

    private final OrderStreamerJpaRepository orderRepository;

    /**
     * payment-events 토픽을 소비하여 주문 상태를 갱신한다.
     * COMPLETED → orders.status = ORDERED
     * FAILED    → orders.status = PAYMENT_FAILED
     */
    @Transactional
    @KafkaListener(topics = "payment-events", containerFactory = KafkaConfig.BATCH_LISTENER)
    public void handlePaymentEvents(
        List<ConsumerRecord<String, PaymentEventMessage>> messages,
        Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, PaymentEventMessage> record : messages) {
            PaymentEventMessage message = record.value();
            if (message == null) {
                continue;
            }
            processPaymentEvent(message);
        }
        acknowledgment.acknowledge();
    }

    private void processPaymentEvent(PaymentEventMessage message) {
        if (!"PAYMENT_RESULT".equals(message.type())) {
            log.warn("알 수 없는 PaymentEvent 타입: {}", message.type());
            return;
        }

        String orderStatus = switch (message.status()) {
            case "COMPLETED" -> "ORDERED";
            case "FAILED" -> "PAYMENT_FAILED";
            default -> {
                log.warn("알 수 없는 결제 상태: {}", message.status());
                yield null;
            }
        };

        if (orderStatus == null) {
            return;
        }

        int affected = orderRepository.updateOrderStatus(message.orderId(), orderStatus);
        if (affected == 0) {
            log.warn("주문 상태 업데이트 실패 (row 없음): orderId={}", message.orderId());
        }
    }

    public record PaymentEventMessage(String type, Long orderId, String status) {
    }
}
