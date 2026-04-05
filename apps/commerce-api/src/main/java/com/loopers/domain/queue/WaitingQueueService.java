package com.loopers.domain.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private static final String QUEUE_KEY = "waiting-queue";
    private static final String SEQ_KEY = "waiting-queue:seq";

    /**
     * INCR로 유니크 score 발급 후 ZADD NX + ZRANK 를 원자적으로 실행하는 Lua 스크립트.
     * INCR: 항상 유니크한 단조 증가 score 생성 → 동시 진입 시 score 충돌 및 사전순 정렬 충돌 방지
     * ZADD NX: 이미 존재하면 갱신 없이 유지 (순번 보존)
     * ZRANK: 0-based rank 반환
     */
    private static final RedisScript<Long> ENTER_SCRIPT = RedisScript.of(
        "local score = redis.call('INCR', KEYS[2])\n" +
        "redis.call('ZADD', KEYS[1], 'NX', score, ARGV[1])\n" +
        "return redis.call('ZRANK', KEYS[1], ARGV[1])",
        Long.class
    );

    @Qualifier("redisTemplateMaster")
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 대기열에 진입한다. 이미 진입한 경우 현재 순번을 그대로 반환한다.
     * Lua 스크립트로 ZADD NX + ZRANK 를 원자적으로 실행하여 race condition 방지.
     *
     * @return 1-based 순번 (1 = 첫 번째 대기자)
     */
    public long enter(Long userId) {
        try {
            Long rank = redisTemplate.execute(
                ENTER_SCRIPT,
                List.of(QUEUE_KEY, SEQ_KEY),
                String.valueOf(userId)
            );
            if (rank == null) {
                throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 사용할 수 없습니다.");
            }
            return rank + 1;
        } catch (DataAccessException e) {
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 사용할 수 없습니다.");
        }
    }

    /**
     * 현재 순번을 조회한다.
     * ZRANK: 0-based → +1하여 1-based 반환
     *
     * @return 1-based 순번, 대기열에 없으면 Optional.empty()
     */
    public Optional<Long> getPositionOptional(Long userId) {
        try {
            Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, String.valueOf(userId));
            return Optional.ofNullable(rank).map(r -> r + 1);
        } catch (DataAccessException e) {
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 사용할 수 없습니다.");
        }
    }

    /**
     * 현재 순번을 조회한다.
     *
     * @return 1-based 순번
     */
    public long getPosition(Long userId) {
        return getPositionOptional(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "대기열에 존재하지 않는 사용자입니다."));
    }

    /**
     * 전체 대기 인원을 조회한다. (ZCARD)
     */
    public long getTotalCount() {
        try {
            Long count = redisTemplate.opsForZSet().size(QUEUE_KEY);
            return count == null ? 0L : count;
        } catch (DataAccessException e) {
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 사용할 수 없습니다.");
        }
    }

    /**
     * 대기열 앞에서 N명을 꺼낸다. (ZPOPMIN) — 스케줄러용
     *
     * @return 꺼낸 userId 목록
     */
    public List<Long> popNext(int count) {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().popMin(QUEUE_KEY, count);
            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }
            return tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .map(Long::valueOf)
                .toList();
        } catch (DataAccessException e) {
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 사용할 수 없습니다.");
        }
    }
}
