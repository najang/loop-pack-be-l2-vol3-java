package com.loopers.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Kafka Consumer 멱등성 보장 — 처리 완료된 이벤트 기록.
 * (topic, event_id) 유니크 제약으로 동일 이벤트 중복 처리를 방지한다.
 */
@Entity
@Table(
    name = "event_handled",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_event_handled_topic_event_id",
        columnNames = {"topic", "event_id"}
    )
)
public class EventHandled {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    protected EventHandled() {
    }

    public EventHandled(String topic, String eventId) {
        this.topic = topic;
        this.eventId = eventId;
    }
}
