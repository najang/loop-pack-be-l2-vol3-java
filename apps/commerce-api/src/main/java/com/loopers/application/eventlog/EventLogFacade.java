package com.loopers.application.eventlog;

import com.loopers.domain.eventlog.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class EventLogFacade {

    private final EventLogRepository eventLogRepository;

    @Transactional(readOnly = true)
    public Page<EventLogInfo> findAll(Pageable pageable) {
        return eventLogRepository.findAll(pageable).map(EventLogInfo::from);
    }
}
