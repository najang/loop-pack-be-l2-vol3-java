package com.loopers.application.eventlog;

import com.loopers.domain.eventlog.EventLog;

import java.time.ZonedDateTime;

public record EventLogInfo(
    Long id,
    String eventType,
    Long userId,
    Long targetId,
    String metadata,
    ZonedDateTime createdAt
) {
    public static EventLogInfo from(EventLog eventLog) {
        return new EventLogInfo(
            eventLog.getId(),
            eventLog.getEventType(),
            eventLog.getUserId(),
            eventLog.getTargetId(),
            eventLog.getMetadata(),
            eventLog.getCreatedAt()
        );
    }
}
