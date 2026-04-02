/**
 * 대기열 전체 플로우 테스트
 *
 * 목적: 진입 → polling → 토큰 수령 → 주문까지의 E2E 흐름 검증
 *      스케줄러가 100ms마다 14명씩 토큰을 발급하는 속도 하에서
 *      실제 유저가 얼마나 기다리는지, 주문까지 성공하는지 확인
 *
 * 실행:
 *   k6 run k6/queue-flow.js
 *   k6 run k6/queue-flow.js --env BASE_URL=http://localhost:8080 --env PRODUCT_ID=1
 *
 * 사전 조건:
 *   - commerce-api 서버 실행 중
 *   - PRODUCT_ID 환경변수로 주문할 상품 ID 지정 (기본값 1)
 *   - 해당 상품이 SELLING 상태이고 재고 충분해야 함
 *   - 테스트 유저들이 DB에 등록되어 있어야 함
 *     (혹은 setup()의 유저 등록 API 활용)
 *
 * 주요 지표:
 *   - wait_time_seconds: 진입부터 토큰 수령까지 실제 대기 시간
 *   - order_success_rate: 토큰 수령 후 주문 성공률
 *   - polling_count: 토큰 수령까지 polling 횟수
 *   - flow_completion_rate: 전체 플로우 완주율
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { BASE_URL, authHeaders, authHeadersWithToken } from './utils/helpers.js';

const waitTimeTrend = new Trend('wait_time_seconds', true);
const pollingCountTrend = new Trend('polling_count');
const orderSuccessRate = new Rate('order_success_rate');
const flowCompletionRate = new Rate('flow_completion_rate');
const tokenExpiredCount = new Counter('token_expired_count');

const PRODUCT_ID = parseInt(__ENV.PRODUCT_ID || '1');
const MAX_POLL_ATTEMPTS = 300;   // 최대 polling 횟수 (5분 = 300초)
const CARD_TYPE = 'SAMSUNG';
const CARD_NO = '1234-5678-9012-3456';

export const options = {
    scenarios: {
        // 30명이 동시에 대기열에 진입해 전체 플로우를 완주
        queue_flow: {
            executor: 'ramping-vus',
            stages: [
                { duration: '5s', target: 30 },    // 30명 진입
                { duration: '300s', target: 30 },  // 모두 플로우 완주까지 유지
                { duration: '5s', target: 0 },
            ],
        },
    },
    thresholds: {
        // 전체 플로우 완주율 90% 이상
        'flow_completion_rate': ['rate>0.9'],
        // 주문 성공률 95% 이상 (토큰 수령에 성공한 경우 기준)
        'order_success_rate': ['rate>0.95'],
        // 대기 시간 중앙값 60초 이내 (30명 / 14TPS ≈ 2초 이론치, 여유 있게 설정)
        'wait_time_seconds': ['med<60'],
    },
};

export default function () {
    const loginId = `loaduser${__VU}`;
    const password = 'Test1234!';

    // ── Step 1: 대기열 진입 ──────────────────────────────────────────
    const enterRes = http.post(
        `${BASE_URL}/api/v1/queue/enter`,
        null,
        { headers: authHeaders(loginId, password) }
    );

    const enterOk = check(enterRes, {
        '[enter] status 200': (r) => r.status === 200,
        '[enter] has position': (r) => r.json()?.data?.position > 0,
    });

    if (!enterOk) {
        console.error(`[VU ${__VU}] 대기열 진입 실패: ${enterRes.status} ${enterRes.body}`);
        flowCompletionRate.add(false);
        return;
    }

    const enterTime = Date.now();
    let pollingCount = 0;
    let entryToken = null;

    // ── Step 2: 순번 polling → 토큰 수령 ────────────────────────────
    for (let i = 0; i < MAX_POLL_ATTEMPTS; i++) {
        const posRes = http.get(
            `${BASE_URL}/api/v1/queue/position`,
            { headers: authHeaders(loginId, password) }
        );

        pollingCount++;

        if (posRes.status === 404) {
            // 대기열에서 이미 제거된 경우 (비정상)
            console.warn(`[VU ${__VU}] 대기열 조회 404 - 대기열에서 제거됨`);
            break;
        }

        check(posRes, {
            '[position] status 200': (r) => r.status === 200,
        });

        const data = posRes.json()?.data;
        if (!data) break;

        if (data.token !== null && data.token !== undefined) {
            entryToken = data.token;
            break;
        }

        // recommendedPollingIntervalSeconds 준수
        const interval = data.recommendedPollingIntervalSeconds || 1;
        sleep(interval);
    }

    pollingCountTrend.add(pollingCount);

    if (!entryToken) {
        console.warn(`[VU ${__VU}] 토큰 수령 실패 (polling ${pollingCount}회 소진)`);
        tokenExpiredCount.add(1);
        flowCompletionRate.add(false);
        return;
    }

    const waitSeconds = (Date.now() - enterTime) / 1000;
    waitTimeTrend.add(waitSeconds);

    // ── Step 3: 입장 토큰으로 주문 ──────────────────────────────────
    const orderPayload = JSON.stringify({
        productId: PRODUCT_ID,
        quantity: 1,
        couponId: null,
        cardType: CARD_TYPE,
        cardNo: CARD_NO,
    });

    const orderRes = http.post(
        `${BASE_URL}/api/v1/orders`,
        orderPayload,
        { headers: authHeadersWithToken(loginId, password, entryToken) }
    );

    const orderOk = check(orderRes, {
        '[order] status 202': (r) => r.status === 202,
        '[order] has orderId': (r) => r.json()?.data?.id !== undefined,
    });

    orderSuccessRate.add(orderOk);
    flowCompletionRate.add(orderOk);

    if (!orderOk) {
        console.error(`[VU ${__VU}] 주문 실패: ${orderRes.status} ${orderRes.body}`);
    }
}