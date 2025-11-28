import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ==================== Custom Metrics ====================
const errorRate = new Rate('error_rate');
const postCreationDuration = new Trend('post_creation_duration');
const commentCreationDuration = new Trend('comment_creation_duration');
const postListDuration = new Trend('post_list_duration');
const dbErrors = new Counter('db_errors');
const concurrencyErrors = new Counter('concurrency_errors');
const activeVUs = new Gauge('active_vus');

// ==================== Configuration ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/community`;

// Test data configuration
const MIN_POST_ID = 1;
const MAX_POST_ID = parseInt(__ENV.MAX_POST_ID) || 1394;

// ==================== ⚡ QUICK TEST (3분) - 현실적인 혼합 워크로드 ====================
// 🎯 목적: 개발 중 빠른 피드백을 위한 짧은 테스트
// ⏱️ 총 시간: 3분
//
// 🎯 핵심 개선사항:
// 1. ✅ 사용자 페르소나별 시나리오 분리 (조회, 상호작용, 작성)
// 2. ✅ 모든 작업이 동시에 실행 (현실적인 혼합 워크로드)
// 3. ✅ Sleep 최소화 및 더 빠른 반복
// 4. ✅ 실제 사용자 비율 반영
//
// 📌 사용 시나리오:
//    - 코드 수정 후 즉시 성능 확인
//    - PR 생성 전 빠른 검증
//    - 로컬 개발 환경에서 자주 실행
//
// 🚀 실행 방법:
//    k6 run k6/community/V2_test_quick.js
//
export const options = {
    scenarios: {
        // ========================================
        // 1️⃣ Smoke Test: 기본 기능 검증 (30초)
        // ========================================
        smoke_test: {
            executor: 'constant-vus',
            vus: 1,
            duration: '30s',
            exec: 'smokeTest',
            tags: { test_type: 'smoke' },
        },

        // ========================================
        // 2️⃣ Lurker Scenario (70% - 조회만)
        // ========================================
        lurker_scenario: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 100 },    // 빠른 증가
                { duration: '30s', target: 140 },   // 피크
                { duration: '30s', target: 0 },     // 종료
            ],
            exec: 'lurkerFlow',
            tags: { test_type: 'lurker', user_type: 'reader' },
        },

        // ========================================
        // 3️⃣ Active User Scenario (20% - 조회 + 상호작용)
        // ========================================
        active_user_scenario: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 500 },
                { duration: '30s', target: 500 },
                { duration: '30s', target: 0 },
            ],
            exec: 'activeUserFlow',
            tags: { test_type: 'active', user_type: 'engaged_reader' },
        },

        // ========================================
        // 4️⃣ Interactive Scenario (8% - 좋아요/댓글 집중)
        // ========================================
        interactive_scenario: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 100 },
                { duration: '30s', target: 300 },
                { duration: '30s', target: 0 },
            ],
            exec: 'interactiveFlow',
            tags: { test_type: 'interactive', user_type: 'contributor' },
        },

        // ========================================
        // 5️⃣ Creator Scenario (2% - 게시글/댓글 작성)
        // ========================================
        creator_scenario: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 200 },
                { duration: '30s', target: 200 },
                { duration: '30s', target: 0 },
            ],
            exec: 'creatorFlow',
            tags: { test_type: 'creator', user_type: 'content_creator' },
        },

        // ========================================
        // 6️⃣ Spike Scenario (트래픽 급증)
        // ========================================
        spike_scenario: {
            executor: 'ramping-vus',
            startTime: '1m30s',  // 중간에 갑자기 발생
            stages: [
                { duration: '10s', target: 150 },   // 급증!
                { duration: '30s', target: 200 },   // 유지
                { duration: '20s', target: 0 },     // 급감
            ],
            exec: 'spikeFlow',
            tags: { test_type: 'spike', user_type: 'viral_traffic' },
        },
    },

    // ========================================
    // 📊 성능 목표 (완화된 기준)
    // ========================================
    thresholds: {
        http_req_duration: ['p(95)<2000'],     // 2초 이내
        http_req_failed: ['rate<0.1'],         // 실패율 10% 이하 (빠른 테스트라 완화)
        error_rate: ['rate<0.1'],
        concurrency_errors: ['count<100'],     // 동시성 에러 100건 이하
    },
};

// ==================== Setup & Teardown ====================
export function setup() {
    console.log('⚡ Quick Test setup: Verifying API health');
    const res = http.get(`${API_BASE}/posts?page=0&size=1`);

    if (res.status !== 200) {
        throw new Error(`API health check failed: ${res.status}`);
    }

    return {
        startTime: new Date().toISOString(),
        baseUrl: API_BASE,
    };
}

export function teardown(data) {
    console.log(`⚡ Quick Test completed. Started at: ${data.startTime}`);
}

// ==================== Helper Functions ====================
function getHeaders() {
    return { 'Content-Type': 'application/json' };
}

function getMultipartHeaders() {
    return {};
}

function checkResponse(res, options = {}) {
    const { tag = 'unknown', expectStatus = 200, allowNotFound = false } = options;

    const checks = {
        'response time < 3s': (r) => r.timings.duration < 3000,
    };

    if (allowNotFound) {
        checks['status is 2xx or 404'] = (r) => (r.status >= 200 && r.status < 300) || r.status === 404;
    } else {
        checks['status is expected'] = (r) => r.status === expectStatus;
    }

    const success = check(res, checks);

    if (res.status >= 500) {
        errorRate.add(1);
        dbErrors.add(1);

        if (res.body) {
            const body = String(res.body);
            if (body.includes('Deadlock') || body.includes('Lock') || body.includes('concurrent')) {
                concurrencyErrors.add(1);
                console.warn(`[${tag}] Concurrency issue detected: ${res.status}`);
            }
        }
    } else {
        errorRate.add(0);
    }

    return success;
}

function getRandomPostId() {
    if (Math.random() < 0.8) {
        return randomIntBetween(MIN_POST_ID, Math.min(200, MAX_POST_ID));
    } else {
        return randomIntBetween(MIN_POST_ID, MAX_POST_ID);
    }
}

function createPost() {
    const formData = {
        request: http.file(JSON.stringify({
            title: `Quick Test Post ${Date.now()}`,
            content: `Quick test at ${new Date().toISOString()}`,
            category: randomItem(['자유게시판', '질문', '정보공유', '공지'])
        }), 'request.json', 'application/json')
    };

    const res = http.post(`${API_BASE}/posts`, formData, {
        headers: getMultipartHeaders(),
        tags: { name: 'create_post' },
    });

    checkResponse(res, { tag: 'CreatePost', expectStatus: 201 });
    postCreationDuration.add(res.timings.duration);

    if (res.status === 201 && res.json('postId')) {
        return res.json('postId');
    }
    return null;
}

function createComment(postId) {
    const payload = JSON.stringify({
        content: `Quick test comment ${Date.now()}`
    });

    const res = http.post(`${API_BASE}/posts/${postId}/comments`, payload, {
        headers: getHeaders(),
        tags: { name: 'create_comment' },
    });

    checkResponse(res, { tag: 'CreateComment', expectStatus: 201 });
    commentCreationDuration.add(res.timings.duration);

    return res.status === 201;
}

/**
 * 📌 좋아요 토글 헬퍼 함수
 *
 * 💡 현실적인 사용자 행동 반영:
 *    - 70% 확률: 한번만 좋아요 (좋아요 추가)
 *    - 30% 확률: 2-3번 토글 (좋아요 추가 → 취소 → 추가)
 *
 * @param {number} postId - 게시글 ID
 * @param {object} options - { tag, allowMultiple }
 */
function toggleLike(postId, options = {}) {
    const { tag = 'like', allowMultiple = true } = options;

    // 70% 확률로 한번만, 30% 확률로 2-3번 토글
    const shouldToggleMultiple = allowMultiple && Math.random() < 0.3;
    const toggleCount = shouldToggleMultiple ? randomIntBetween(2, 3) : 1;

    for (let i = 0; i < toggleCount; i++) {
        const res = http.post(`${API_BASE}/posts/${postId}/likes`, null, {
            headers: getHeaders(),
            tags: { name: tag },
        });
        checkResponse(res, { tag: 'Like' });

        // 여러번 토글할 때 사이에 짧은 대기
        if (toggleCount > 1 && i < toggleCount - 1) {
            sleep(randomIntBetween(0.1, 0.3));
        }
    }
}

// ==================== Test Scenarios ====================

/**
 * 1️⃣ Smoke Test (30초)
 */
export function smokeTest() {
    group('Quick Smoke Test', () => {
        let res = http.get(`${API_BASE}/posts?page=0&size=10`, {
            headers: getHeaders(),
            tags: { name: 'list_posts' },
        });
        checkResponse(res, { tag: 'ListPosts' });
        postListDuration.add(res.timings.duration);
        sleep(0.5);

        const postId = getRandomPostId();
        res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'get_post' },
        });
        checkResponse(res, { tag: 'GetPost', allowNotFound: true });
        sleep(0.5);
    });
}

/**
 * 2️⃣ Lurker Flow: 조회만 하는 사용자 (70%)
 */
export function lurkerFlow() {
    activeVUs.add(1);

    group('Quick Lurker', () => {
        // 목록 조회
        let res = http.get(`${API_BASE}/posts?page=${randomIntBetween(0, 2)}&size=10`, {
            headers: getHeaders(),
            tags: { name: 'lurker_browse' },
        });
        checkResponse(res, { tag: 'LurkerBrowse' });
        postListDuration.add(res.timings.duration);

        sleep(randomIntBetween(0.3, 1));  // 빠른 스크롤

        // 게시글 조회 (1-2개)
        const numPosts = randomIntBetween(1, 2);
        for (let i = 0; i < numPosts; i++) {
            const postId = getRandomPostId();
            res = http.get(`${API_BASE}/posts/${postId}`, {
                headers: getHeaders(),
                tags: { name: 'lurker_read' },
            });
            checkResponse(res, { tag: 'LurkerRead', allowNotFound: true });

            sleep(randomIntBetween(0.3, 1));  // 빠르게 읽고 이동
        }
    });
}

/**
 * 3️⃣ Active User Flow: 조회 + 간단한 상호작용 (20%)
 */
export function activeUserFlow() {
    activeVUs.add(1);

    group('Quick Active User', () => {
        const postId = getRandomPostId();
        let res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'active_read' },
        });
        checkResponse(res, { tag: 'ActiveRead', allowNotFound: true });

        if (res.status === 200) {
            sleep(randomIntBetween(0.5, 1));  // 읽는 시간

            // 댓글 조회
            res = http.get(`${API_BASE}/posts/${postId}/comments`, {
                headers: getHeaders(),
                tags: { name: 'active_read_comments' },
            });
            checkResponse(res, { tag: 'ActiveReadComments' });

            sleep(randomIntBetween(0.3, 0.8));

            // 50% 확률로 좋아요 (70%는 한번만, 30%는 2-3번 토글)
            if (Math.random() < 0.5) {
                toggleLike(postId, { tag: 'active_like', allowMultiple: true });
                sleep(0.3);
            }
        } else {
            sleep(randomIntBetween(0.5, 1));
        }
    });
}

/**
 * 4️⃣ Interactive Flow: 좋아요/댓글 상호작용 집중 (8%)
 */
export function interactiveFlow() {
    group('Quick Interactive', () => {
        const hotPostId = randomIntBetween(1, 10);

        const action = Math.random();

        if (action < 0.6) {
            // 60%: 좋아요 토글 (동시성 압박! 30% 확률로 2-3번 토글)
            toggleLike(hotPostId, { tag: 'interactive_like', allowMultiple: true });
        } else if (action < 0.85) {
            // 25%: 조회수 증가
            const res = http.post(`${API_BASE}/posts/${hotPostId}/views`, null, {
                headers: getHeaders(),
                tags: { name: 'interactive_view' },
            });
            checkResponse(res, { tag: 'InteractiveView' });
        } else {
            // 15%: 댓글 작성
            createComment(hotPostId);
        }

        sleep(randomIntBetween(0.3, 0.8));  // 빠른 반복
    });
}

/**
 * 5️⃣ Creator Flow: 게시글/댓글 작성 (2%)
 */
export function creatorFlow() {
    group('Quick Creator', () => {
        const action = Math.random();

        if (action < 0.7) {
            // 70%: 게시글 작성
            createPost();
            sleep(randomIntBetween(1, 2));
        } else {
            // 30%: 댓글 작성
            const postId = getRandomPostId();
            createComment(postId);
            sleep(randomIntBetween(0.5, 1));
        }
    });
}

/**
 * 6️⃣ Spike Flow: 급격한 트래픽 증가 (바이럴)
 */
export function spikeFlow() {
    group('Quick Spike', () => {
        const postId = getRandomPostId();

        const res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'spike_read' },
        });
        checkResponse(res, { tag: 'SpikeRead', allowNotFound: true });

        if (res.status === 200 && Math.random() < 0.7) {
            // 바이럴 상황: 70% 확률로 좋아요 (일부는 토글)
            toggleLike(postId, { tag: 'spike_like', allowMultiple: true });
        }

        sleep(randomIntBetween(0.2, 0.6));  // 매우 빠른 연속 액션
    });
}
