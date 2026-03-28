package com.loopers.application.order;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transactional Outbox Relay.
 * 1초마다 미발행 OutboxEvent를 조회하여 Kafka에 발행하고 published=true로 마킹한다.
 * - FOR UPDATE SKIP LOCKED: 다중 인스턴스 환경에서 중복 발행 방지
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Transactional
    @Scheduled(fixedDelay = 1000)
    public void relay() {
        List<OutboxEvent> events = outboxRepository.findUnpublished();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload());
                event.markPublished();
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("OutboxEvent 발행 실패: eventId={}, topic={}", event.getEventId(), event.getTopic(), e);
            }
        }
    }
}
