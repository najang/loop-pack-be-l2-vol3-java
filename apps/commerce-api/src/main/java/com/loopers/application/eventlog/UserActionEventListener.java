package com.loopers.application.eventlog;

import com.loopers.domain.event.UserActionEvent;
import com.loopers.domain.eventlog.EventLog;
import com.loopers.domain.eventlog.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserActionEventListener {

    private final EventLogRepository eventLogRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
