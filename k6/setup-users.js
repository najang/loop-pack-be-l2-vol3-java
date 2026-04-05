/**
 * 부하 테스트용 유저 사전 등록 스크립트
 *
 * 실행:
 *   k6 run k6/setup-users.js                        # 기본 200명
 *   k6 run k6/setup-users.js --env COUNT=30         # 30명만
 *   k6 run k6/setup-users.js --env BASE_URL=http://localhost:8080
 *
 * 결과:
 *   loaduser_1 ~ loaduser_N 유저가 DB에 등록됨
 *   이미 등록된 유저는 409로 skip (재실행 안전)
 */

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from './utils/helpers.js';

const COUNT = parseInt(__ENV.COUNT || '200');

export const options = {
    vus: 1,
    iterations: 1,
};

export default function () {
    let created = 0;
    let skipped = 0;
    let failed = 0;

    for (let i = 1; i <= COUNT; i++) {
        const payload = JSON.stringify({
            loginId: `loaduser${i}`,
            password: 'Test1234!',
            name: `부하테스트유저${i}`,
            birthDate: '1995-01-01',
            email: `loaduser${i}@test.com`,
        });

        const res = http.post(
            `${BASE_URL}/api/v1/users`,
            payload,
            { headers: { 'Content-Type': 'application/json' } }
        );

        if (res.status === 200 || res.status === 201) {
            created++;
        } else if (res.status === 409) {
            // 이미 존재하는 유저 — 재실행 시 정상
            skipped++;
        } else {
            console.error(`[${i}] 등록 실패: ${res.status} ${res.body}`);
            failed++;
        }
    }

    console.log(`완료 — 등록: ${created}, 스킵(중복): ${skipped}, 실패: ${failed}`);

    check(null, {
        '등록 실패 없음': () => failed === 0,
    });
}