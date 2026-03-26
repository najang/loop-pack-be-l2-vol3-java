package com.loopers.interfaces.api.eventlog;

import com.loopers.application.eventlog.EventLogInfo;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

public class EventLogAdminV1Dto {

    public record EventLogResponse(
        Long id,
        String eventType,
        Long userId,
        Long targetId,
        String metadata,
        ZonedDateTime createdAt
    ) {
        public static EventLogResponse from(EventLogInfo info) {
            return new EventLogResponse(
                info.id(),
                info.eventType(),
                info.userId(),
                info.targetId(),
                info.metadata(),
                info.createdAt()
            );
        }
    }

    public record EventLogPageResponse(List<EventLogResponse> content, int page, int size, long totalElements) {
        public static EventLogPageResponse from(Page<EventLogInfo> page) {
            List<EventLogResponse> content = page.getContent().stream()
                .map(EventLogResponse::from)
                .toList();
            return new EventLogPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements());
        }
    }
}
