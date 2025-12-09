import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ==================== Custom Metrics ====================
const errorRate = new Rate('error_rate');
const successRate = new Rate('success_rate');

// Operation Trends
const postViewTrend = new Trend('post_view_duration', true);
const postListTrend = new Trend('post_list_duration', true);
const postCreateTrend = new Trend('post_create_duration', true);
const commentCreateTrend = new Trend('comment_create_duration', true);
const likeToggleTrend = new Trend('like_toggle_duration', true);
const viewCountTrend = new Trend('view_count_duration', true);

// Operation Counters
const reads = new Counter('read_operations');
const writes = new Counter('write_operations');

// ==================== Configuration ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/community`;

const MIN_POST_ID = parseInt(__ENV.MIN_POST_ID) || 1;
const MAX_POST_ID = parseInt(__ENV.MAX_POST_ID) || 205;
const MIN_USER_ID = parseInt(__ENV.MIN_USER_ID) || 2;
const MAX_USER_ID = parseInt(__ENV.MAX_USER_ID) || 500;

// ==================== 실제 트래픽 비율 ====================
// 실제 게시판 트래픽: 읽기 90% / 쓰기 10%
// 읽기: 목록 30%, 상세 60%
// 쓰기: 좋아요 60%, 댓글 30%, 게시글 10%
const TRAFFIC_WEIGHTS = {
    // 읽기 (90%)
    POST_LIST: 27,      // 30% of 90%
    POST_VIEW: 54,      // 60% of 90%
    VIEW_COUNT: 9,      // 조회수 증가 (조회와 함께)

    // 쓰기 (10%)
    LIKE_TOGGLE: 6,     // 60% of 10%
    COMMENT_CREATE: 3,  // 30% of 10%
    POST_CREATE: 1,     // 10% of 10%
};

const TOTAL_WEIGHT = Object.values(TRAFFIC_WEIGHTS).reduce((a, b) => a + b, 0);

// ==================== Test Options ====================
export const options = {
    scenarios: {
        // 1️⃣ Warm-up: 시스템 예열
        warmup: {
            executor: 'constant-arrival-rate',
            rate: 50,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 30,
            maxVUs: 100,
            exec: 'realisticTraffic',
            tags: { phase: 'warmup' },
        },

        // 2️⃣ Ramp-up: 점진적 부하 증가
        rampup: {
            executor: 'ramping-arrival-rate',
            startTime: '30s',
            startRate: 50,
            timeUnit: '1s',
            stages: [
                { duration: '30s', target: 100 },
                { duration: '30s', target: 200 },
                { duration: '30s', target: 300 },
                { duration: '30s', target: 400 },
                { duration: '30s', target: 500 },
            ],
            preAllocatedVUs: 100,
            maxVUs: 600,
            exec: 'realisticTraffic',
            tags: { phase: 'rampup' },
        },

        // 3️⃣ Peak: 최대 부하 유지
        peak: {
            executor: 'constant-arrival-rate',
            startTime: '3m',
            rate: 500,
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 200,
            maxVUs: 800,
            exec: 'realisticTraffic',
            tags: { phase: 'peak' },
        },

        // 4️⃣ Spike: 순간 스파이크 테스트
        spike: {
            executor: 'ramping-arrival-rate',
            startTime: '5m',
            startRate: 500,
            timeUnit: '1s',
            stages: [
                { duration: '10s', target: 1000 },  // 스파이크!
                { duration: '20s', target: 1000 },  // 유지
                { duration: '10s', target: 500 },   // 복구
            ],
            preAllocatedVUs: 300,
            maxVUs: 1200,
            exec: 'realisticTraffic',
            tags: { phase: 'spike' },
        },

        // 5️⃣ Cooldown: 부하 감소
        cooldown: {
            executor: 'ramping-arrival-rate',
            startTime: '5m40s',
            startRate: 500,
            timeUnit: '1s',
            stages: [
                { duration: '20s', target: 100 },
            ],
            preAllocatedVUs: 50,
            maxVUs: 200,
            exec: 'realisticTraffic',
            tags: { phase: 'cooldown' },
        },
    },

    thresholds: {
        // 전체 성능 기준
        http_req_duration: ['p(50)<500', 'p(95)<2000', 'p(99)<5000'],
        http_req_failed: ['rate<0.05'],
        error_rate: ['rate<0.05'],
        success_rate: ['rate>0.95'],

        // 읽기 작업 (엄격)
        post_view_duration: ['p(95)<1000'],
        post_list_duration: ['p(95)<1500'],

        // 쓰기 작업 (관대)
        like_toggle_duration: ['p(95)<1500'],
        comment_create_duration: ['p(95)<3000'],
        post_create_duration: ['p(95)<5000'],
    },
};

// ==================== Setup ====================
export function setup() {
    console.log('🚀 Realistic Max RPS Test');
    console.log('═══════════════════════════════════════════');
    console.log(`📍 BASE_URL: ${BASE_URL}`);
    console.log(`📊 Post ID Range: ${MIN_POST_ID} ~ ${MAX_POST_ID}`);
    console.log(`👥 User ID Range: ${MIN_USER_ID} ~ ${MAX_USER_ID}`);
    console.log('');
    console.log('📈 Traffic Distribution:');
    console.log(`   읽기 90%: 목록(${TRAFFIC_WEIGHTS.POST_LIST}%) + 상세(${TRAFFIC_WEIGHTS.POST_VIEW}%) + 조회수(${TRAFFIC_WEIGHTS.VIEW_COUNT}%)`);
    console.log(`   쓰기 10%: 좋아요(${TRAFFIC_WEIGHTS.LIKE_TOGGLE}%) + 댓글(${TRAFFIC_WEIGHTS.COMMENT_CREATE}%) + 게시글(${TRAFFIC_WEIGHTS.POST_CREATE}%)`);
    console.log('');
    console.log('⏱️ Test Phases:');
    console.log('   0:00-0:30  Warmup (50 RPS)');
    console.log('   0:30-3:00  Ramp-up (50→500 RPS)');
    console.log('   3:00-5:00  Peak (500 RPS)');
    console.log('   5:00-5:40  Spike (1000 RPS)');
    console.log('   5:40-6:00  Cooldown');
    console.log('═══════════════════════════════════════════');

    // Health check
    const res = http.get(`${API_BASE}/posts?page=0&size=1`);
    if (res.status !== 200) {
        throw new Error(`Health check failed: ${res.status}`);
    }
    console.log('✅ Health check passed\n');

    return { startTime: new Date().toISOString() };
}

export function teardown(data) {
    console.log(`\n🏁 Test completed. Started at: ${data.startTime}`);
}

// ==================== Helper Functions ====================
function getHeaders() {
    return { 'Content-Type': 'application/json' };
}

function getRandomUserId() {
    return randomIntBetween(MIN_USER_ID, MAX_USER_ID);
}

function getRandomPostId() {
    // 80% 확률로 인기 게시글 (상위 20%)
    if (Math.random() < 0.8) {
        const hotRange = Math.max(1, Math.floor((MAX_POST_ID - MIN_POST_ID + 1) * 0.2));
        return randomIntBetween(MAX_POST_ID - hotRange + 1, MAX_POST_ID);
    }
    return randomIntBetween(MIN_POST_ID, MAX_POST_ID);
}

function recordResult(res, trend, isWrite = false) {
    const success = res.status >= 200 && res.status < 400;

    check(res, {
        'status is success': (r) => success,
        'response time < 10s': (r) => r.timings.duration < 10000,
    });

    trend.add(res.timings.duration);
    errorRate.add(!success);
    successRate.add(success);

    if (isWrite) {
        writes.add(1);
    } else {
        reads.add(1);
    }

    return success;
}

// ==================== API Operations ====================

// 📖 게시글 목록 조회
function getPostList() {
    const page = randomIntBetween(0, 10);
    const res = http.get(`${API_BASE}/posts?page=${page}&size=10`, {
        headers: getHeaders(),
        tags: { name: 'GET /posts' },
    });
    recordResult(res, postListTrend);
}

// 📖 게시글 상세 조회
function getPost() {
    const postId = getRandomPostId();
    const res = http.get(`${API_BASE}/posts/${postId}`, {
        headers: getHeaders(),
        tags: { name: 'GET /posts/:id' },
    });
    recordResult(res, postViewTrend);
}

// 📖 조회수 증가
function increaseViewCount() {
    const postId = getRandomPostId();
    const res = http.post(`${API_BASE}/posts/${postId}/views`, null, {
        headers: getHeaders(),
        tags: { name: 'POST /posts/:id/views' },
    });
    recordResult(res, viewCountTrend);
}

// ✏️ 좋아요 토글
function toggleLike() {
    const postId = getRandomPostId();
    const userId = getRandomUserId();
    const res = http.post(`${API_BASE}/posts/${postId}/likes?testUserId=${userId}`, null, {
        headers: getHeaders(),
        tags: { name: 'POST /posts/:id/likes' },
    });
    recordResult(res, likeToggleTrend, true);
}

// ✏️ 댓글 작성
function createComment() {
    const postId = getRandomPostId();
    const userId = getRandomUserId();
    const payload = JSON.stringify({
        content: `Load test comment ${Date.now()}`,
    });
    const res = http.post(`${API_BASE}/posts/${postId}/comments?testUserId=${userId}`, payload, {
        headers: getHeaders(),
        tags: { name: 'POST /posts/:id/comments' },
    });
    recordResult(res, commentCreateTrend, true);
}

// ✏️ 게시글 작성
function createPost() {
    const userId = getRandomUserId();
    const categories = ['자유게시판', '질문', '정보공유'];

    const formData = {
        request: http.file(
            JSON.stringify({
                title: `Load Test Post ${Date.now()}`,
                content: `Performance test content created at ${new Date().toISOString()}`,
                category: randomItem(categories),
            }),
            'request.json',
            'application/json'
        ),
    };

    const res = http.post(`${API_BASE}/posts?testUserId=${userId}`, formData, {
        tags: { name: 'POST /posts' },
    });
    recordResult(res, postCreateTrend, true);
}

// ==================== Traffic Distribution ====================
function selectOperation() {
    const rand = Math.random() * TOTAL_WEIGHT;
    let cumulative = 0;

    cumulative += TRAFFIC_WEIGHTS.POST_LIST;
    if (rand < cumulative) return 'POST_LIST';

    cumulative += TRAFFIC_WEIGHTS.POST_VIEW;
    if (rand < cumulative) return 'POST_VIEW';

    cumulative += TRAFFIC_WEIGHTS.VIEW_COUNT;
    if (rand < cumulative) return 'VIEW_COUNT';

    cumulative += TRAFFIC_WEIGHTS.LIKE_TOGGLE;
    if (rand < cumulative) return 'LIKE_TOGGLE';

    cumulative += TRAFFIC_WEIGHTS.COMMENT_CREATE;
    if (rand < cumulative) return 'COMMENT_CREATE';

    return 'POST_CREATE';
}

// ==================== Main Test Function ====================
export function realisticTraffic() {
    const operation = selectOperation();

    switch (operation) {
        case 'POST_LIST':
            getPostList();
            break;
        case 'POST_VIEW':
            getPost();
            break;
        case 'VIEW_COUNT':
            increaseViewCount();
            break;
        case 'LIKE_TOGGLE':
            toggleLike();
            break;
        case 'COMMENT_CREATE':
            createComment();
            break;
        case 'POST_CREATE':
            createPost();
            break;
    }
}

// ==================== 읽기 전용 테스트 (비교용) ====================
export function readOnlyTraffic() {
    const rand = Math.random();

    if (rand < 0.3) {
        getPostList();
    } else if (rand < 0.9) {
        getPost();
    } else {
        increaseViewCount();
    }
}
