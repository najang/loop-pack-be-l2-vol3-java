package com.loopers.application.eventlog;

import com.loopers.domain.event.UserActionEvent;
import com.loopers.domain.eventlog.EventLog;
import com.loopers.domain.eventlog.EventLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActionEventListenerTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @InjectMocks
    private UserActionEventListener userActionEventListener;

    @DisplayName("handle() 시,")
    @Nested
    class Handle {

        @DisplayName("정상 이벤트이면, EventLog를 저장한다.")
        @Test
        void savesEventLog_whenValidEvent() {
            // arrange
            UserActionEvent event = new UserActionEvent(
                UserActionEvent.EventType.PRODUCT_VIEWED, 1L, 100L, null
            );

            // act
            userActionEventListener.handle(event);

            // assert
            verify(eventLogRepository, times(1)).save(argThat(log ->
                log.getEventType().equals("PRODUCT_VIEWED")
                    && log.getUserId().equals(1L)
                    && log.getTargetId().equals(100L)
                    && log.getMetadata() == null
            ));
        }

        @DisplayName("metadata가 있으면, metadata도 함께 저장한다.")
        @Test
        void savesMetadata_whenProvided() {
            // arrange
            String metadata = "{\"productId\":100,\"quantity\":2}";
            UserActionEvent event = new UserActionEvent(
                UserActionEvent.EventType.ORDER_CREATED, 1L, 300L, metadata
            );

            // act
            userActionEventListener.handle(event);

            // assert
            verify(eventLogRepository, times(1)).save(argThat(log ->
                log.getMetadata().equals(metadata)
            ));
        }

        @DisplayName("save() 실패 시, 예외가 전파되지 않는다.")
        @Test
        void doesNotPropagateException_whenSaveFails() {
            // arrange
            UserActionEvent event = new UserActionEvent(
                UserActionEvent.EventType.LIKED, 1L, 100L, null
            );
            when(eventLogRepository.save(org.mockito.ArgumentMatchers.any(EventLog.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

            // act & assert
            assertThatCode(() -> userActionEventListener.handle(event))
                .doesNotThrowAnyException();
        }
    }
}
