package com.loopers.domain.eventlog;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "event_log")
public class EventLog extends BaseEntity {

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public EventLog(String eventType, Long userId, Long targetId, String metadata) {
        this.eventType = eventType;
        this.userId = userId;
        this.targetId = targetId;
        this.metadata = metadata;
    }
}
