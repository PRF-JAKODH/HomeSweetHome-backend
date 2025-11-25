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
const MAX_POST_ID = parseInt(__ENV.MAX_POST_ID) || 500;  // 게시글 수 현실적으로 조정

// ==================== Test Scenarios ====================
export const options = {
    scenarios: {
        // Smoke Test: 기본 기능 검증 (1 VU, 1분)
        smoke_test: {
            executor: 'constant-vus',
            vus: 1,
            duration: '1m',
            exec: 'smokeTest',
            tags: { test_type: 'smoke' },
        },

        // Load Test: 평균 부하 테스트 (DAU 30만 기준)
        load_test: {
            executor: 'ramping-vus',
            startTime: '1m',
            stages: [
                { duration: '3m', target: 50 },   // Ramp-up to normal
                { duration: '5m', target: 50 },   // Normal traffic
                { duration: '2m', target: 100 },  // Peak time
                { duration: '5m', target: 100 },  // Sustained peak
                { duration: '2m', target: 150 },  // High peak
                { duration: '3m', target: 150 },  // High peak sustained
                { duration: '2m', target: 0 },    // Ramp-down
            ],
            exec: 'normalUserFlow',
            tags: { test_type: 'load' },
        },

        // Stress Test: 동시성 제어 검증 (같은 리소스에 집중)
        stress_test: {
            executor: 'ramping-vus',
            startTime: '23m',
            stages: [
                { duration: '2m', target: 100 },  // Quick ramp-up
                { duration: '5m', target: 100 },  // Sustained stress
                { duration: '2m', target: 200 },  // High stress
                { duration: '3m', target: 200 },  // Peak stress
                { duration: '1m', target: 0 },    // Ramp-down
            ],
            exec: 'concurrentAccessTest',
            tags: { test_type: 'stress' },
        },

        // Spike Test: 급격한 트래픽 증가 (이벤트/이슈 발생 시)
        spike_test: {
            executor: 'ramping-vus',
            startTime: '36m',
            stages: [
                { duration: '30s', target: 300 },  // Sudden spike (viral content)
                { duration: '2m', target: 300 },   // Hold spike
                { duration: '30s', target: 0 },    // Sudden drop
            ],
            exec: 'spikeUserFlow',
            tags: { test_type: 'spike' },
        },
    },

    // SLA-based thresholds
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        http_req_failed: ['rate<0.01'],
        error_rate: ['rate<0.01'],
        post_list_duration: ['p(95)<500'],
        post_creation_duration: ['p(95)<1500'],
        comment_creation_duration: ['p(95)<1000'],
        concurrency_errors: ['count<10'],
    },
};

// ==================== Setup & Teardown ====================
export function setup() {
    console.log('Test setup: Verifying API health');
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
    console.log(`Test completed. Started at: ${data.startTime}`);
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

    // 404는 allowNotFound가 true일 때 성공으로 간주
    if (allowNotFound) {
        checks['status is 2xx or 404'] = (r) => (r.status >= 200 && r.status < 300) || r.status === 404;
    } else {
        checks['status is expected'] = (r) => r.status === expectStatus;
    }

    const success = check(res, checks);

    // 5xx 에러만 실제 에러로 카운팅
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
    // Pareto principle: 80% of traffic goes to 20% of posts
    if (Math.random() < 0.8) {
        // Hot posts (top 20%): 80% of requests
        return randomIntBetween(MIN_POST_ID, Math.min(200, MAX_POST_ID));
    } else {
        // All posts: 20% of requests
        return randomIntBetween(MIN_POST_ID, MAX_POST_ID);
    }
}

function createPost() {
    const formData = {
        request: http.file(JSON.stringify({
            title: `Performance Test Post ${Date.now()}`,
            content: `This is a test post created during load testing at ${new Date().toISOString()}`,
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
        content: `Test comment created at ${Date.now()}`
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

// Smoke Test: 기본 기능 검증
export function smokeTest() {
    group('Smoke Test - Basic Functionality', () => {
        // 1. List posts
        let res = http.get(`${API_BASE}/posts?page=0&size=10`, {
            headers: getHeaders(),
            tags: { name: 'list_posts' },
        });
        checkResponse(res, { tag: 'ListPosts' });
        postListDuration.add(res.timings.duration);
        sleep(1);

        // 2. Get post detail
        const postId = getRandomPostId();
        res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'get_post' },
        });
        checkResponse(res, { tag: 'GetPost', allowNotFound: true });
        sleep(1);

        // 3. Get comments (only if post exists)
        if (res.status === 200) {
            res = http.get(`${API_BASE}/posts/${postId}/comments`, {
                headers: getHeaders(),
                tags: { name: 'get_comments' },
            });
            checkResponse(res, { tag: 'GetComments' });
        }
        sleep(1);

        // 4. Create post
        createPost();
        sleep(2);

        // 5. Create comment (only if post exists)
        if (res.status === 200) {
            createComment(postId);
        }
        sleep(1);
    });
}

// Normal User Flow: 일반적인 사용자 행동 패턴
export function normalUserFlow() {
    activeVUs.add(1);

    group('Normal User Journey', () => {
        // Browse posts
        let res = http.get(`${API_BASE}/posts?page=${randomIntBetween(0, 5)}&size=10`, {
            headers: getHeaders(),
            tags: { name: 'browse_posts' },
        });
        checkResponse(res, { tag: 'BrowsePosts' });
        postListDuration.add(res.timings.duration);

        sleep(randomIntBetween(2, 5));

        // Read random post
        const postId = getRandomPostId();
        res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'read_post' },
        });
        checkResponse(res, { tag: 'ReadPost', allowNotFound: true });

        // Only continue if post exists
        if (res.status === 200) {
            sleep(randomIntBetween(3, 8));

            // Read comments
            res = http.get(`${API_BASE}/posts/${postId}/comments`, {
                headers: getHeaders(),
                tags: { name: 'read_comments' },
            });
            checkResponse(res, { tag: 'ReadComments' });

            sleep(randomIntBetween(2, 5));

            // 40% chance to like post (현실적: 좋아요는 많이 누름)
            if (Math.random() < 0.4) {
                res = http.post(`${API_BASE}/posts/${postId}/likes`, null, {
                    headers: getHeaders(),
                    tags: { name: 'like_post' },
                });
                checkResponse(res, { tag: 'LikePost' });
                sleep(1);
            }

            // 8% chance to create comment (현실적: 댓글은 적게 작성)
            if (Math.random() < 0.08) {
                createComment(postId);
                sleep(randomIntBetween(1, 3));
            }
        }

        // 3% chance to create post (현실적: 게시글 작성은 더 적음)
        if (Math.random() < 0.03) {
            createPost();
            sleep(randomIntBetween(2, 4));
        }
    });
}

// Concurrent Access Test: 동시성 제어 검증 (좋아요 토글 집중)
export function concurrentAccessTest() {
    group('Concurrent Access - Same Resource', () => {
        // Target hot posts (1-10) to trigger concurrency control
        const hotPostId = randomIntBetween(1, 10);

        const action = Math.random();

        if (action < 0.7) {
            // Like toggle (70% - 가장 빈번한 동시성 이슈)
            const res = http.post(`${API_BASE}/posts/${hotPostId}/likes`, null, {
                headers: getHeaders(),
                tags: { name: 'concurrent_like' },
            });
            checkResponse(res, { tag: 'ConcurrentLike' });
        } else if (action < 0.9) {
            // View count (20%)
            const res = http.post(`${API_BASE}/posts/${hotPostId}/views`, null, {
                headers: getHeaders(),
                tags: { name: 'concurrent_view' },
            });
            checkResponse(res, { tag: 'ConcurrentView' });
        } else {
            // Comment (10%)
            createComment(hotPostId);
        }

        sleep(randomIntBetween(1, 2));  // 더 빠른 연속 요청
    });
}

// Spike User Flow: 급격한 트래픽 증가 시 사용자 행동 (바이럴 컨텐츠)
export function spikeUserFlow() {
    group('Spike Traffic', () => {
        // Simplified flow for spike scenario
        const postId = getRandomPostId();

        // Quick read
        const res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'spike_read' },
        });
        checkResponse(res, { tag: 'SpikeRead', allowNotFound: true });

        // Quick interaction (only if post exists) - 바이럴 시 좋아요 폭증
        if (res.status === 200 && Math.random() < 0.6) {
            http.post(`${API_BASE}/posts/${postId}/likes`, null, {
                headers: getHeaders(),
                tags: { name: 'spike_like' },
            });
        }

        sleep(randomIntBetween(0.5, 1.5));  // 빠른 연속 액션
    });
}
