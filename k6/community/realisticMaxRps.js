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
// 부하 테스트용: 읽기 80% / 쓰기 20%
// 읽기: 목록 조회 50%, 게시글 조회 50% (조회수 증가 API 포함)
// 쓰기: 좋아요 50%, 댓글 작성 30%, 게시글 작성 20%
const TRAFFIC_WEIGHTS = {
    // 읽기 (80%)
    POST_LIST: 40,      // 50% of 80%
    POST_VIEW: 40,      // 50% of 80% (상세 조회 시 조회수 증가도 함께 호출)

    // 쓰기 (20%)
    LIKE_TOGGLE: 10,    // 50% of 20%
    COMMENT_CREATE: 6,  // 30% of 20%
    POST_CREATE: 4,     // 20% of 20%
};

const TOTAL_WEIGHT = Object.values(TRAFFIC_WEIGHTS).reduce((a, b) => a + b, 0);

// ==================== Test Options ====================
// 🎯 DAU 30만 기준 (t3.xlarge WAS, t3.xlarge DB, r6g.medium Redis)
// - Peak 1000 RPS 달성 (1600 VUs), Spike 2000 RPS 목표
// - 목표: p(95) < 500ms, Error rate < 1%
export const options = {
    scenarios: {
        // 1️⃣ Warm-up: 시스템 예열 (캐시 워밍, 커넥션 풀 준비)
        warmup: {
            executor: 'constant-arrival-rate',
            rate: 50,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 100,
            maxVUs: 100,
            exec: 'realisticTraffic',
            tags: { phase: 'warmup' },
        },

        // 2️⃣ Ramp-up: 점진적 부하 증가 (50 → 800 RPS)
        rampup: {
            executor: 'ramping-arrival-rate',
            startTime: '30s',
            startRate: 50,
            timeUnit: '1s',
            stages: [
                { duration: '30s', target: 300 },   // 일반 트래픽
                { duration: '30s', target: 600 },   // 피크 시간대 진입
                { duration: '30s', target: 800 },   // 피크 트래픽
                { duration: '30s', target: 100 },   // 쿨다운
            ],
            preAllocatedVUs: 300,
            maxVUs: 800,
            exec: 'realisticTraffic',
            tags: { phase: 'rampup' },
        },

        // 3️⃣ Peak: 안정 부하 유지 (500 RPS × 3분)
        peak: {
            executor: 'constant-arrival-rate',
            startTime: '2m30s',
            rate: 500,
            timeUnit: '1s',
            duration: '3m',
            preAllocatedVUs: 200,
            maxVUs: 1000,
            exec: 'realisticTraffic',
            tags: { phase: 'peak' },
        },

        // 4️⃣ Spike: 이벤트/푸시 알림 시나리오 (최대 2400 RPS 목표)
        spike: {
            executor: 'ramping-arrival-rate',
            startTime: '5m30s',
            startRate: 500,
            timeUnit: '1s',
            stages: [
                { duration: '15s', target: 2000 },  // 급격한 스파이크 (실제 ~1000 RPS 달성)
                { duration: '30s', target: 2400 },  // 최대 버스트 목표 (VU 한계로 ~1000 RPS)
                { duration: '15s', target: 1000 },   // 정상 복구
            ],
            preAllocatedVUs: 800,
            maxVUs: 1600,
            exec: 'realisticTraffic',
            tags: { phase: 'spike' },
        },

        // 5️⃣ Stabilize: 스파이크 후 안정화 확인
        stabilize: {
            executor: 'constant-arrival-rate',
            startTime: '6m30s',
            rate: 1000,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 400,
            maxVUs: 800,
            exec: 'realisticTraffic',
            tags: { phase: 'stabilize' },
        },

        // 6️⃣ Cooldown: 부하 감소
        cooldown: {
            executor: 'ramping-arrival-rate',
            startTime: '7m30s',
            startRate: 500,
            timeUnit: '1s',
            stages: [
                { duration: '15s', target: 400 },
                { duration: '15s', target: 100 },
            ],
            preAllocatedVUs: 50,
            maxVUs: 200,
            exec: 'realisticTraffic',
            tags: { phase: 'cooldown' },
        },
    },

    thresholds: {
        // 전체 성능 기준 (DAU 30만 서비스 수준)
        http_req_duration: ['p(50)<200', 'p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.01'],      // 에러율 1% 미만
        error_rate: ['rate<0.01'],
        success_rate: ['rate>0.99'],

        // 읽기 작업 (엄격 - 사용자 체감 중요)
        post_view_duration: ['p(95)<300'],   // 상세 조회 300ms 이내
        post_list_duration: ['p(95)<400'],   // 목록 조회 400ms 이내

        // 쓰기 작업 (상대적으로 관대)
        like_toggle_duration: ['p(95)<500'],     // 좋아요 500ms 이내
        comment_create_duration: ['p(95)<800'],  // 댓글 800ms 이내
        post_create_duration: ['p(95)<1500'],    // 게시글 작성 1.5초 이내
    },
};

// ==================== Setup ====================
export function setup() {
    console.log('🚀 DAU 30만 기준 부하 테스트');
    console.log('═══════════════════════════════════════════════════');
    console.log('🖥️  인프라: WAS t3.xlarge | DB t3.xlarge | Redis r6g.medium');
    console.log(`📍 BASE_URL: ${BASE_URL}`);
    console.log(`📊 Post ID Range: ${MIN_POST_ID} ~ ${MAX_POST_ID}`);
    console.log(`👥 User ID Range: ${MIN_USER_ID} ~ ${MAX_USER_ID}`);
    console.log('');
    console.log('📈 Traffic Distribution:');
    console.log(`   읽기 80%: 목록 조회(${TRAFFIC_WEIGHTS.POST_LIST}%) + 게시글 조회(${TRAFFIC_WEIGHTS.POST_VIEW}%)`);
    console.log(`   쓰기 20%: 좋아요(${TRAFFIC_WEIGHTS.LIKE_TOGGLE}%) + 댓글(${TRAFFIC_WEIGHTS.COMMENT_CREATE}%) + 게시글(${TRAFFIC_WEIGHTS.POST_CREATE}%)`);
    console.log('');
    console.log('⏱️ Test Phases (총 8분):');
    console.log('   0:00-0:30  ① Warmup (50 RPS)');
    console.log('   0:30-2:30  ② Ramp-up (50→800 RPS)');
    console.log('   2:30-5:30  ③ Peak (500 RPS 유지)');
    console.log('   5:30-6:30  ④ Spike (2000→2400 RPS 목표, 실제 ~1000 RPS)');
    console.log('   6:30-7:30  ⑤ Stabilize (1000 RPS 복구 확인)');
    console.log('   7:30-8:00  ⑥ Cooldown (500→100 RPS)');
    console.log('');
    console.log('🎯 Success Criteria:');
    console.log('   p(95) < 500ms | Error rate < 1% | Peak 1000 RPS 달성');
    console.log('═══════════════════════════════════════════════════');

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

// 📖 게시글 상세 조회 + 조회수 증가
// 조회수를 별도 API로 분리하면 캐싱 전략이 유연해짐
// (게시글 내용은 캐시, 조회수는 비동기/배치 처리 가능)
function getPost() {
    const postId = getRandomPostId();

    // 1. 상세 조회
    const detailRes = http.get(`${API_BASE}/posts/${postId}`, {
        headers: getHeaders(),
        tags: { name: 'GET /posts/:id' },
    });
    recordResult(detailRes, postViewTrend);

    // 2. 조회수 증가 (실제 사용자 행동: 상세 페이지 진입 시 조회수 +1)
    const viewRes = http.post(`${API_BASE}/posts/${postId}/views`, null, {
        headers: getHeaders(),
        tags: { name: 'POST /posts/:id/views' },
    });
    recordResult(viewRes, viewCountTrend);
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
            getPost();  // 상세 조회 + 조회수 증가
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

    if (rand < 0.5) {
        getPostList();      // 목록 조회 50%
    } else {
        getPost();          // 게시글 조회 50% (조회수 증가 포함)
    }
}
