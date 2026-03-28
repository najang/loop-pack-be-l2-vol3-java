package com.loopers.domain.eventlog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class EventLogTest {

    @DisplayName("EventLog 생성 시,")
    @Nested
    class Create {

        @DisplayName("모든 필드가 올바르게 설정된다.")
        @Test
        void setsAllFieldsCorrectly() {
            // act
            EventLog eventLog = new EventLog("PRODUCT_VIEWED", 1L, 100L, "{\"source\":\"detail\"}");

            // assert
            assertAll(
                () -> assertThat(eventLog.getEventType()).isEqualTo("PRODUCT_VIEWED"),
                () -> assertThat(eventLog.getUserId()).isEqualTo(1L),
                () -> assertThat(eventLog.getTargetId()).isEqualTo(100L),
                () -> assertThat(eventLog.getMetadata()).isEqualTo("{\"source\":\"detail\"}")
            );
        }

        @DisplayName("metadata가 null이어도 생성할 수 있다.")
        @Test
        void allowsNullMetadata() {
            // act
            EventLog eventLog = new EventLog("LIKED", 1L, 100L, null);

            // assert
            assertThat(eventLog.getMetadata()).isNull();
        }
    }
}
