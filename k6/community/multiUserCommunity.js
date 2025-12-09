import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ==================== Custom Metrics ====================
const errorRate = new Rate('error_rate');
const postCreationDuration = new Trend('post_creation_duration');
const postViewDuration = new Trend('post_view_duration');
const commentCreationDuration = new Trend('comment_creation_duration');
const likeToggleDuration = new Trend('like_toggle_duration');
const viewCountDuration = new Trend('view_count_duration');
const postListDuration = new Trend('post_list_duration');
const dbErrors = new Counter('db_errors');
const concurrencyErrors = new Counter('concurrency_errors');
const activeVUs = new Gauge('active_vus');

// Operation counters
const postCreations = new Counter('post_creations');
const postViews = new Counter('post_views');
const commentCreations = new Counter('comment_creations');
const likeOperations = new Counter('like_operations');
const viewCountOperations = new Counter('view_count_operations');

// ==================== Configuration ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/community`;

// ==================== Test Data Configuration ====================
const MIN_POST_ID = parseInt(__ENV.MIN_POST_ID) || 1;
const MAX_POST_ID = parseInt(__ENV.MAX_POST_ID) || 205;
const MIN_USER_ID = parseInt(__ENV.MIN_USER_ID) || 2;
const MAX_USER_ID = parseInt(__ENV.MAX_USER_ID) || 500;

// ==================== VU 300 부하 테스트 ====================
// 현재 인프라 적정 부하 (t3.medium + t3a.small)
export const options = {
    scenarios: {
        // 0️⃣ Smoke Test
        smoke_test: {
            executor: 'constant-vus',
            vus: 1,
            duration: '10s',
            exec: 'smokeTest',
            tags: { test_type: 'smoke' },
        },

        // 1️⃣ Lurker (70%) - 조회만
        lurker_scenario: {
            executor: 'ramping-vus',
            startTime: '10s',
            stages: [
                { duration: '30s', target: 105 },   // 워밍업
                { duration: '1m', target: 210 },    // 피크 (70% of 300)
                { duration: '1m', target: 210 },    // 유지
                { duration: '30s', target: 0 },     // 종료
            ],
            exec: 'lurkerFlow',
            tags: { test_type: 'lurker' },
        },

        // 2️⃣ Active Reader (15%) - 조회 + 좋아요
        active_reader_scenario: {
            executor: 'ramping-vus',
            startTime: '10s',
            stages: [
                { duration: '30s', target: 23 },
                { duration: '1m', target: 45 },     // 피크 (15% of 300)
                { duration: '1m', target: 45 },
                { duration: '30s', target: 0 },
            ],
            exec: 'activeReaderFlow',
            tags: { test_type: 'active_reader' },
        },

        // 3️⃣ Commenter (10%) - 조회 + 좋아요 + 댓글
        commenter_scenario: {
            executor: 'ramping-vus',
            startTime: '10s',
            stages: [
                { duration: '30s', target: 15 },
                { duration: '1m', target: 30 },     // 피크 (10% of 300)
                { duration: '1m', target: 30 },
                { duration: '30s', target: 0 },
            ],
            exec: 'commenterFlow',
            tags: { test_type: 'commenter' },
        },

        // 4️⃣ Creator (5%) - 게시글 작성
        creator_scenario: {
            executor: 'ramping-vus',
            startTime: '10s',
            stages: [
                { duration: '30s', target: 8 },
                { duration: '1m', target: 15 },     // 피크 (5% of 300)
                { duration: '1m', target: 15 },
                { duration: '30s', target: 0 },
            ],
            exec: 'creatorFlow',
            tags: { test_type: 'creator' },
        },
    },

    thresholds: {
        http_req_duration: ['p(95)<2000', 'p(99)<5000'],  // 목표 완화
        http_req_failed: ['rate<0.10'],                   // 10% 이하
        error_rate: ['rate<0.10'],
        concurrency_errors: ['count<100'],
        post_view_duration: ['p(95)<1500'],
        like_toggle_duration: ['p(95)<1000'],
        comment_creation_duration: ['p(95)<2000'],
        post_creation_duration: ['p(95)<3000'],
    },
};

// ==================== Setup & Teardown ====================
export function setup() {
    console.log('🚀 Community Load Test (VU 300)');
    console.log('========================================');
    console.log(`📝 BASE_URL: ${BASE_URL}`);
    console.log(`📝 Post ID Range: ${MIN_POST_ID} ~ ${MAX_POST_ID}`);
    console.log('📊 VU Distribution (Total 300):');
    console.log('   - Lurker: 210, Reader: 45, Commenter: 30, Creator: 15');
    console.log('========================================');

    const res = http.get(`${API_BASE}/posts?page=0&size=1`);
    if (res.status !== 200) {
        throw new Error(`API health check failed: ${res.status}`);
    }
    console.log('✅ API health check passed');

    return { startTime: new Date().toISOString() };
}

export function teardown(data) {
    console.log(`\n🏁 Test completed. Started at: ${data.startTime}`);
}

// ==================== Helper Functions ====================
function getHeaders() { return { 'Content-Type': 'application/json' }; }

function getUniqueUserId() {
    const vuId = __VU || 1;
    return MIN_USER_ID + (vuId % (MAX_USER_ID - MIN_USER_ID + 1));
}

function getRandomPostId() {
    if (Math.random() < 0.8) {
        const hotRange = Math.max(1, Math.floor((MAX_POST_ID - MIN_POST_ID + 1) * 0.2));
        return randomIntBetween(MIN_POST_ID, MIN_POST_ID + hotRange - 1);
    }
    return randomIntBetween(MIN_POST_ID, MAX_POST_ID);
}

function checkResponse(res, options = {}) {
    const { expectStatus = 200, allowNotFound = false } = options;
    const checks = { 'response time < 5s': (r) => r.timings.duration < 5000 };

    if (allowNotFound) {
        checks['status is 2xx or 404'] = (r) => (r.status >= 200 && r.status < 300) || r.status === 404;
    } else {
        checks['status is expected'] = (r) => r.status === expectStatus;
    }

    check(res, checks);
    errorRate.add(res.status >= 500 ? 1 : 0);
    if (res.status >= 500) dbErrors.add(1);
    return res.status === expectStatus;
}

// ==================== API Functions ====================
function getPostList(page = 0) {
    const res = http.get(`${API_BASE}/posts?page=${page}&size=10`, { headers: getHeaders() });
    checkResponse(res);
    postListDuration.add(res.timings.duration);
    return res;
}

function getPost(postId) {
    const res = http.get(`${API_BASE}/posts/${postId}`, { headers: getHeaders() });
    checkResponse(res, { allowNotFound: true });
    postViewDuration.add(res.timings.duration);
    postViews.add(1);
    return res;
}

function increaseViewCount(postId) {
    const res = http.post(`${API_BASE}/posts/${postId}/views`, null, { headers: getHeaders() });
    checkResponse(res);
    viewCountDuration.add(res.timings.duration);
    viewCountOperations.add(1);
    return res;
}

function togglePostLike(postId, userId) {
    const res = http.post(`${API_BASE}/posts/${postId}/likes?testUserId=${userId}`, null, { headers: getHeaders() });
    check(res, { 'like toggle success': (r) => r.status === 200 || r.status === 201 });
    likeToggleDuration.add(res.timings.duration);
    likeOperations.add(1);
    return res;
}

function getComments(postId) {
    const res = http.get(`${API_BASE}/posts/${postId}/comments`, { headers: getHeaders() });
    checkResponse(res);
    return res;
}

function createComment(postId, userId) {
    const payload = JSON.stringify({ content: `Test comment by user ${userId} at ${Date.now()}` });
    const res = http.post(`${API_BASE}/posts/${postId}/comments`, payload, { headers: getHeaders() });
    checkResponse(res, { expectStatus: 201 });
    commentCreationDuration.add(res.timings.duration);
    commentCreations.add(1);
    return res;
}

function createPost(userId) {
    const categories = ['자유게시판', '질문', '정보공유', '공지'];
    const formData = {
        request: http.file(JSON.stringify({
            title: `Performance Test Post ${Date.now()}`,
            content: `Test post at ${new Date().toISOString()}`,
            category: randomItem(categories)
        }), 'request.json', 'application/json')
    };
    const res = http.post(`${API_BASE}/posts`, formData);
    checkResponse(res, { expectStatus: 201 });
    postCreationDuration.add(res.timings.duration);
    postCreations.add(1);
    return res;
}

// ==================== Test Scenarios ====================

export function smokeTest() {
    group('Smoke Test', () => {
        getPostList(0);
        sleep(0.5);
        getPost(getRandomPostId());
        sleep(0.5);
    });
}

export function lurkerFlow() {
    activeVUs.add(1);
    group('Lurker', () => {
        getPostList(randomIntBetween(0, 10));
        sleep(randomIntBetween(1, 2));

        const numPosts = randomIntBetween(2, 3);
        for (let i = 0; i < numPosts; i++) {
            const postId = getRandomPostId();
            const res = getPost(postId);
            if (res.status === 200) {
                increaseViewCount(postId);
                if (Math.random() < 0.5) {
                    sleep(0.5);
                    getComments(postId);
                }
            }
            sleep(randomIntBetween(1, 3));
        }
    });
}

export function activeReaderFlow() {
    activeVUs.add(1);
    const userId = getUniqueUserId();
    group('Active Reader', () => {
        getPostList(randomIntBetween(0, 5));
        sleep(randomIntBetween(1, 2));

        const postId = getRandomPostId();
        const res = getPost(postId);
        if (res.status === 200) {
            increaseViewCount(postId);
            sleep(1);
            getComments(postId);
            if (Math.random() < 0.7) {
                togglePostLike(postId, userId);
            }
        }
        sleep(randomIntBetween(2, 4));
    });
}

export function commenterFlow() {
    activeVUs.add(1);
    const userId = getUniqueUserId();
    group('Commenter', () => {
        getPostList(randomIntBetween(0, 5));
        sleep(1);

        const postId = getRandomPostId();
        const res = getPost(postId);
        if (res.status === 200) {
            increaseViewCount(postId);
            sleep(1);
            getComments(postId);
            if (Math.random() < 0.8) togglePostLike(postId, userId);
            if (Math.random() < 0.6) createComment(postId, userId);
        }
        sleep(randomIntBetween(3, 5));
    });
}

export function creatorFlow() {
    activeVUs.add(1);
    const userId = getUniqueUserId();
    group('Creator', () => {
        if (Math.random() < 0.4) {
            createPost(userId);
            sleep(randomIntBetween(3, 5));
        } else {
            const postId = getRandomPostId();
            const res = getPost(postId);
            if (res.status === 200) {
                increaseViewCount(postId);
                createComment(postId, userId);
                if (Math.random() < 0.8) togglePostLike(postId, userId);
            }
            sleep(randomIntBetween(2, 4));
        }
    });
}