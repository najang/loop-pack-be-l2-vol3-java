package com.loopers.application.like;

import com.loopers.domain.event.LikeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class LikeEventKafkaPublisher {

    private static final String TOPIC = "catalog-events";

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    /**
     * 좋아요/취소 트랜잭션이 커밋된 후 비동기로 Kafka에 발행한다.
     * - AFTER_COMMIT: 핵심 트랜잭션 롤백 시 이벤트 발행하지 않음
     * - @Async: 별도 스레드에서 실행하여 API 응답 지연 방지
     * - key=productId: 같은 상품 이벤트가 동일 파티션으로 → 순서 보장
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(LikeEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.productId()), event);
    }
}
