package com.loopers.application.queue;

import com.loopers.domain.queue.EntryTokenService;
import com.loopers.domain.queue.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 대기열 입장 토큰 발급 스케줄러
 *
 * <p><b>배치 크기 산정 근거 (Thundering Herd 완화)</b>
 * <ul>
 *   <li>DB 커넥션 풀: 40 (local jpa.yml maximum-pool-size 실제값)</li>
 *   <li>주문 평균 처리 시간: 154ms (k6 VU=1 실측 med)</li>
 *   <li>이론적 최대 TPS = 40 / 0.154 ≈ 259</li>
 *   <li>안전 마진 70% ≈ 181 TPS</li>
 *   <li>스케줄러 주기: 100ms → 배치 크기 = 181 × 0.1 ≈ 18명</li>
 * </ul>
 * 1초에 181명을 한 번에 발급하는 대신, 100ms마다 18명씩 나누어 발급함으로써
 * Thundering Herd로 인한 순간 부하 스파이크를 완화한다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class WaitingQueueScheduler {

    private static final int BATCH_SIZE = 18;

    private final WaitingQueueService waitingQueueService;
    private final EntryTokenService entryTokenService;

    @Scheduled(fixedDelay = 100)
    public void issueEntryTokens() {
        try {
            List<Long> userIds = waitingQueueService.popNext(BATCH_SIZE);
            for (Long userId : userIds) {
                try {
                    entryTokenService.issue(userId);
                } catch (Exception e) {
                    log.warn("입장 토큰 발급 실패: userId={}", userId, e);
                }
            }
        } catch (Exception e) {
            log.error("대기열 스케줄러 실행 실패", e);
        }
    }
}
