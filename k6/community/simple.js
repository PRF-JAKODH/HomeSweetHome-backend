import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ==================== Configuration ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/community`;

// 테스트할 게시글 ID 범위 (특정 게시물에 집중 테스트)
const MIN_POST_ID = 1855;
const MAX_POST_ID = 1865;

// ==================== Test Configuration ====================
export const options = {
    scenarios: {
        // 조회수, 댓글수, 좋아요만 테스트
        interaction_test: {
            executor: 'ramping-vus',
            stages: [
                { duration: '30s', target: 50 },   // 30초 동안 50명까지 증가
                { duration: '1m', target: 50 },    // 1분 동안 50명 유지
                { duration: '30s', target: 0 },    // 30초 동안 0명으로 감소
            ],
            exec: 'testInteractions',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1500'],     // 95% 요청이 1.5초 이내
        http_req_failed: ['rate<0.05'],        // 실패율 5% 이하
    },
};

// ==================== Helper Functions ====================
function getHeaders() {
    return { 'Content-Type': 'application/json' };
}

function getRandomPostId() {
    return randomIntBetween(MIN_POST_ID, MAX_POST_ID);
}

// ==================== Main Test ====================
export function testInteractions() {
    const postId = getRandomPostId();

    // 🔥 핵심: 매번 랜덤한 userId 사용 (1~500 범위)
    // 좋아요가 토글 방식이므로, 다양한 userId를 사용해야 카운트가 증가함!
    const userId = randomIntBetween(2, 100);

    // 랜덤하게 하나의 액션 선택
    const action = Math.random();

    if (action < 1) {
        // 좋아요 토글 - 이제 각 가상 유저마다 다른 userId를 사용!
        // testUserId 쿼리 파라미터로 전달하면 여러 유저의 좋아요가 누적됨
        const res = http.post(`${API_BASE}/posts/${postId}/likes?testUserId=${userId}`, null, {
            headers: getHeaders(),
            tags: {name: 'like_toggle'},
        });

        check(res, {
            'like toggle success': (r) => r.status === 200 || r.status === 201,
        });

        sleep(randomIntBetween(0.5, 2));  // 0.5~2초 대기
    }
}
