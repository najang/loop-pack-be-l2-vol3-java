package com.loopers.infrastructure.eventlog;

import com.loopers.domain.eventlog.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventLogJpaRepository extends JpaRepository<EventLog, Long> {
}
