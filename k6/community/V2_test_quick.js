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

// ==================== ⚡ QUICK TEST (3분) ====================
// 🎯 목적: 개발 중 빠른 피드백을 위한 짧은 테스트
// ⏱️ 총 시간: 3분
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
        // 2️⃣ Load Test: 평균 부하 (1분 30초)
        // ========================================
        load_test: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 100 },   // 빠른 증가
                { duration: '30s', target: 500 },   // 피크
                { duration: '30s', target: 0 },    // 종료
            ],
            exec: 'normalUserFlow',
            tags: { test_type: 'load' },
        },

        // ========================================
        // 3️⃣ Spike Test: 급증 (30초)
        // ========================================
        spike_test: {
            executor: 'ramping-vus',
            startTime: '2m',
            stages: [
                { duration: '10s', target: 200 },   // 급증
                { duration: '10s', target: 1000 },   // 유지
                { duration: '10s', target: 0 },    // 종료
            ],
            exec: 'spikeUserFlow',
            tags: { test_type: 'spike' },
        },

        // ========================================
        // 4️⃣ Stress Test: 동시성 (30초)
        // ========================================
        stress_test: {
            executor: 'ramping-vus',
            startTime: '2m30s',
            stages: [
                { duration: '10s', target: 100 },   // 빠른 증가
                { duration: '10s', target: 800 },   // 유지
                { duration: '10s', target: 0 },    // 종료
            ],
            exec: 'concurrentAccessTest',
            tags: { test_type: 'stress' },
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
 * 2️⃣ Normal User Flow (간소화)
 */
export function normalUserFlow() {
    activeVUs.add(1);

    group('Quick User Journey', () => {
        let res = http.get(`${API_BASE}/posts?page=${randomIntBetween(0, 2)}&size=10`, {
            headers: getHeaders(),
            tags: { name: 'browse_posts' },
        });
        checkResponse(res, { tag: 'BrowsePosts' });
        postListDuration.add(res.timings.duration);

        sleep(randomIntBetween(1, 2));  // 짧은 대기

        const postId = getRandomPostId();
        res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'read_post' },
        });
        checkResponse(res, { tag: 'ReadPost', allowNotFound: true });

        if (res.status === 200 && Math.random() < 0.5) {  // 50% 확률
            http.post(`${API_BASE}/posts/${postId}/likes`, null, {
                headers: getHeaders(),
                tags: { name: 'like_post' },
            });
        }

        sleep(randomIntBetween(0.5, 1));  // 짧은 대기
    });
}

/**
 * 3️⃣ Spike User Flow (간소화)
 */
export function spikeUserFlow() {
    group('Quick Spike', () => {
        const postId = getRandomPostId();

        const res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'spike_read' },
        });
        checkResponse(res, { tag: 'SpikeRead', allowNotFound: true });

        if (res.status === 200 && Math.random() < 0.6) {
            http.post(`${API_BASE}/posts/${postId}/likes`, null, {
                headers: getHeaders(),
                tags: { name: 'spike_like' },
            });
        }

        sleep(randomIntBetween(0.3, 0.8));  // 매우 짧은 대기
    });
}

/**
 * 4️⃣ Concurrent Access Test (동시성 집중)
 */
export function concurrentAccessTest() {
    group('Quick Concurrent Access', () => {
        const hotPostId = randomIntBetween(1, 10);  // 핫한 게시글 1-10번

        const action = Math.random();

        if (action < 0.7) {
            // 좋아요 토글 (70%)
            const res = http.post(`${API_BASE}/posts/${hotPostId}/likes`, null, {
                headers: getHeaders(),
                tags: { name: 'concurrent_like' },
            });
            checkResponse(res, { tag: 'ConcurrentLike' });
        } else if (action < 0.9) {
            // 조회수 (20%)
            const res = http.post(`${API_BASE}/posts/${hotPostId}/views`, null, {
                headers: getHeaders(),
                tags: { name: 'concurrent_view' },
            });
            checkResponse(res, { tag: 'ConcurrentView' });
        } else {
            // 댓글 (10%)
            createComment(hotPostId);
        }

        sleep(randomIntBetween(0.5, 1));  // 짧은 대기
    });
}
