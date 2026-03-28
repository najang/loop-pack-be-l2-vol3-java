package com.loopers.domain.eventlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventLogRepository {

    EventLog save(EventLog eventLog);

    Page<EventLog> findAll(Pageable pageable);
}
