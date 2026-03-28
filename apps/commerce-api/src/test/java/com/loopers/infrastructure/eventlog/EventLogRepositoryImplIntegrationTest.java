package com.loopers.infrastructure.eventlog;

import com.loopers.domain.eventlog.EventLog;
import com.loopers.domain.eventlog.EventLogRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class EventLogRepositoryImplIntegrationTest {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("save() 시, id가 생성되고 필드가 올바르게 저장된다.")
    @Test
    void save_persistsEventLog() {
        // arrange
        EventLog eventLog = new EventLog("PRODUCT_VIEWED", 1L, 100L, null);

        // act
        EventLog saved = eventLogRepository.save(eventLog);

        // assert
        assertAll(
            () -> assertThat(saved.getId()).isNotNull(),
            () -> assertThat(saved.getEventType()).isEqualTo("PRODUCT_VIEWED"),
            () -> assertThat(saved.getUserId()).isEqualTo(1L),
            () -> assertThat(saved.getTargetId()).isEqualTo(100L),
            () -> assertThat(saved.getCreatedAt()).isNotNull()
        );
    }

    @DisplayName("findAll() 시, 페이징된 결과를 반환한다.")
    @Test
    void findAll_returnsPaginatedResults() {
        // arrange
        eventLogRepository.save(new EventLog("PRODUCT_VIEWED", 1L, 100L, null));
        eventLogRepository.save(new EventLog("LIKED", 1L, 200L, null));
        eventLogRepository.save(new EventLog("ORDER_CREATED", 2L, 300L, null));

        // act
        Page<EventLog> page = eventLogRepository.findAll(PageRequest.of(0, 2));

        // assert
        assertAll(
            () -> assertThat(page.getContent()).hasSize(2),
            () -> assertThat(page.getTotalElements()).isEqualTo(3),
            () -> assertThat(page.getTotalPages()).isEqualTo(2)
        );
    }
}
