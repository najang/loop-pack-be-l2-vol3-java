/**
 * 대기열 진입 부하 테스트
 *
 * 목적: 동시 사용자가 몰릴 때 Redis Sorted Set 처리량과 순번 정확성 검증
 *
 * 실행:
 *   k6 run k6/queue-enter.js
 *   k6 run k6/queue-enter.js --env BASE_URL=http://localhost:8080
 *
 * 사전 조건:
 *   - commerce-api 서버 실행 중
 *   - 테스트 전 대기열/토큰 Redis 초기화 권장
 *
 * 주요 지표:
 *   - http_req_duration: 순번 조회 응답 시간
 *   - queue_position: 부여된 순번 분포 (중복 없어야 함)
 *   - error_rate: 오류 비율 (0% 목표)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, authHeaders } from './utils/helpers.js';

const errorCount = new Counter('error_count');
const queuePositionTrend = new Trend('queue_position');

export const options = {
    scenarios: {
        // 단계적 부하 증가: 실제 블랙프라이데이 트래픽 패턴 모사
        ramp_up: {
            executor: 'ramping-vus',
            stages: [
                { duration: '10s', target: 50 },   // 워밍업
                { duration: '20s', target: 200 },   // 부하 증가
                { duration: '20s', target: 200 },   // 최대 부하 유지
                { duration: '10s', target: 0 },     // 종료
            ],
        },
    },
    thresholds: {
        // 99%의 요청이 500ms 이내 응답
        http_req_duration: ['p(99)<500'],
        // 오류율 1% 미만
        http_req_failed: ['rate<0.01'],
    },
};

// 각 VU마다 고유한 사용자 ID 사용 (사전에 DB에 등록된 유저 가정)
// 실제 사용 시 setup()에서 유저를 생성하거나, 미리 등록된 유저 pool 활용
export default function () {
    const userId = `loaduser${__VU}`;
    const password = 'Test1234!';

    const res = http.post(
        `${BASE_URL}/api/v1/queue/enter`,
        null,
        { headers: authHeaders(userId, password) }
    );

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'has position': (r) => {
            const body = r.json();
            return body && body.data && body.data.position > 0;
        },
        'token is null (waiting)': (r) => {
            const body = r.json();
            return body && body.data && body.data.token === null;
        },
    });

    if (!success) {
        errorCount.add(1);
    } else {
        const position = res.json().data.position;
        queuePositionTrend.add(position);
    }

    // 실제 유저 행동 모사: 진입 후 잠시 대기
    sleep(1);
}