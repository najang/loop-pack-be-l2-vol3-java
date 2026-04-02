package com.loopers.domain.queue;

import com.loopers.application.queue.WaitingQueueScheduler;
import com.loopers.utils.ConcurrencyTestHelper;
import com.loopers.utils.ConcurrencyTestHelper.ConcurrencyResult;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WaitingQueueServiceTest {

    @MockBean
    private WaitingQueueScheduler waitingQueueScheduler;

    private static final int THREAD_COUNT = 200;

    @Autowired
    private WaitingQueueService waitingQueueService;

    @Autowired
    private EntryTokenService entryTokenService;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @BeforeEach
    void setUp() {
        redisCleanUp.truncateAll();
    }

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @Nested
    @DisplayName("enter - 대기열 진입")
    class Enter {

        @BeforeEach
        void setUp() {
            redisCleanUp.truncateAll();
        }

        @DisplayName("대기열에 진입하면 1-based 순번을 반환한다.")
        @Test
        void enter_returnsOneBasedPosition() {
            // arrange
            Long userId = 1L;

            // act
            long position = waitingQueueService.enter(userId);

            // assert
            assertThat(position).isEqualTo(1L);
        }

        @DisplayName("이미 진입한 유저가 다시 진입하면 순번이 변경되지 않는다.")
        @Test
        void enter_duplicate_keepsSamePosition() {
            // arrange
            waitingQueueService.enter(1L);
            waitingQueueService.enter(2L);
            waitingQueueService.enter(3L);

            // act
            long positionBefore = waitingQueueService.getPosition(1L);
            waitingQueueService.enter(1L);
            long positionAfter = waitingQueueService.getPosition(1L);

            // assert
            assertThat(positionBefore).isEqualTo(positionAfter);
        }

        @DisplayName("동시에 진입해도 모든 유저의 순번이 유일하게 부여된다.")
        @Test
        void enter_concurrent_allPositionsUnique() throws Exception {
            // arrange
            List<Long> positions = new CopyOnWriteArrayList<>();
            List<java.util.concurrent.Callable<Object>> tasks = LongStream.rangeClosed(1, THREAD_COUNT)
                .mapToObj(userId -> (java.util.concurrent.Callable<Object>) () -> {
                    long pos = waitingQueueService.enter(userId);
                    positions.add(pos);
                    return pos;
                })
                .toList();

            // act
            ConcurrencyResult result = ConcurrencyTestHelper.run(tasks);

            // assert
            assertThat(result.successCount()).isEqualTo(THREAD_COUNT);
            assertThat(positions).hasSize(THREAD_COUNT);
            Set<Long> uniquePositions = Set.copyOf(positions);
            assertThat(uniquePositions).hasSize(THREAD_COUNT);
        }
    }

    @Nested
    @DisplayName("getTotalCount - 전체 대기 인원 조회")
    class GetTotalCount {

        @DisplayName("대기열에 진입한 수만큼 전체 인원이 집계된다.")
        @Test
        void getTotalCount_returnsCorrectCount() {
            // arrange
            waitingQueueService.enter(1L);
            waitingQueueService.enter(2L);
            waitingQueueService.enter(3L);

            // act
            long count = waitingQueueService.getTotalCount();

            // assert
            assertThat(count).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("popNext - 앞에서 N명 꺼내기")
    class PopNext {

        @DisplayName("배치 크기보다 많은 유저가 대기 중일 때, 배치 크기만큼만 꺼낸다.")
        @Test
        void popNext_overBatchSize_popsExactBatchSize() {
            // arrange
            int batchSize = 14;
            int totalUsers = 30;
            for (long i = 1; i <= totalUsers; i++) {
                waitingQueueService.enter(i);
            }

            // act
            List<Long> popped = waitingQueueService.popNext(batchSize);

            // assert
            assertThat(popped).hasSize(batchSize);
            assertThat(waitingQueueService.getTotalCount()).isEqualTo(totalUsers - batchSize);
        }

        @DisplayName("꺼낸 유저는 대기열에서 제거된다.")
        @Test
        void popNext_removedFromQueue() {
            // arrange
            waitingQueueService.enter(1L);
            waitingQueueService.enter(2L);

            // act
            waitingQueueService.popNext(1);

            // assert
            assertThat(waitingQueueService.getTotalCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("입장 토큰 만료")
    class EntryTokenExpiry {

        @DisplayName("발급된 토큰은 검증에 성공한다.")
        @Test
        void token_issued_validatesSuccessfully() {
            // arrange
            Long userId = 1L;
            String token = entryTokenService.issue(userId);

            // act & assert
            assertThat(entryTokenService.validate(userId, token)).isTrue();
        }

        @DisplayName("토큰이 삭제되면 검증에 실패한다. (TTL 만료 동작과 동일)")
        @Test
        void token_deleted_validationFails() {
            // arrange
            Long userId = 1L;
            String token = entryTokenService.issue(userId);

            // act
            entryTokenService.delete(userId);

            // assert
            assertThat(entryTokenService.validate(userId, token)).isFalse();
        }

        @DisplayName("존재하지 않는 유저의 토큰 검증은 실패한다.")
        @Test
        void token_notIssued_validationFails() {
            // arrange
            Long userId = 999L;
            String fakeToken = "fake-token";

            // act & assert
            assertThat(entryTokenService.validate(userId, fakeToken)).isFalse();
        }

        @DisplayName("토큰 발급 후 findToken으로 조회할 수 있다.")
        @Test
        void token_issued_canBeFound() {
            // arrange
            Long userId = 1L;
            String issued = entryTokenService.issue(userId);

            // act
            Optional<String> found = entryTokenService.findToken(userId);

            // assert
            assertThat(found).isPresent().contains(issued);
        }
    }
}
