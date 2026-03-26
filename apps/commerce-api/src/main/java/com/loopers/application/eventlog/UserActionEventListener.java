package com.loopers.application.eventlog;

import com.loopers.domain.event.UserActionEvent;
import com.loopers.domain.eventlog.EventLog;
import com.loopers.domain.eventlog.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserActionEventListener {

    private final EventLogRepository eventLogRepository;

    @Async
    @EventListener
    public void handle(UserActionEvent event) {
        try {
            eventLogRepository.save(new EventLog(
                event.eventType().name(),
                event.userId(),
                event.targetId(),
                event.metadata()
            ));
        } catch (Exception e) {
            log.error("이벤트 로그 저장 실패: {}", event, e);
        }
    }
}
