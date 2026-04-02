package com.loopers.interfaces.api.queue;

public class QueueV1Dto {

    /**
     * 대기열 진입 / 순번 조회 공통 응답
     */
    public record QueueStatusResponse(
        long position,
        long estimatedWaitSeconds,
        int recommendedPollingIntervalSeconds,
        String token
    ) {

        /**
         * 대기 중 응답 (토큰 없음)
         */
        public static QueueStatusResponse waiting(long position, long estimatedWaitSeconds) {
            return new QueueStatusResponse(
                position,
                estimatedWaitSeconds,
                recommendedPollingInterval(position),
                null
            );
        }

        /**
         * 입장 가능 응답 (토큰 발급됨)
         */
        public static QueueStatusResponse ready(String token) {
            return new QueueStatusResponse(0, 0, 1, token);
        }

        /**
         * 순번 구간에 따른 권장 Polling 주기 (초)
         * - 1~100:    1초 (곧 입장)
         * - 101~1000: 3초
         * - 1001+:    5초
         */
        private static int recommendedPollingInterval(long position) {
            if (position <= 100) return 1;
            if (position <= 1000) return 3;
            return 5;
        }
    }
}
