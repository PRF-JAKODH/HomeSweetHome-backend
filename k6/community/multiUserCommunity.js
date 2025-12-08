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

// ==================== DAU 30만 기반 부하 테스트 ====================
// 
// 📊 서비스 규모 분석:
//   - DAU: 300,000명
//   - 피크 시간대 동시접속자: ~15,000 ~ 20,000명 (DAU의 5~7%)
//   - 테스트 목표: 2,000 VU (피크의 10~15% 시뮬레이션)
//
// 📊 사용자 행동 패턴 (실제 커뮤니티 서비스 기준):
//   - Lurker (70%): 조회만 하는 사용자
//   - Active Reader (15%): 조회 + 좋아요
//   - Commenter (10%): 조회 + 좋아요 + 댓글
//   - Creator (5%): 게시글 작성 포함 모든 활동
//
// 🚀 실행 방법:
//    k6 run -e BASE_URL=http://your-ec2:8080 \
//           -e MIN_POST_ID=1 \
//           -e MAX_POST_ID=205 \
//           k6/community/multiUserCommunity.js
//
export const options = {
    scenarios: {
        // ========================================
        // 0️⃣ Smoke Test: API 헬스 체크
        // ========================================
        smoke_test: {
            executor: 'constant-vus',
            vus: 1,
            duration: '10s',
            exec: 'smokeTest',
            tags: { test_type: 'smoke' },
        },

        // ========================================
        // 1️⃣ Lurker Scenario (70%) - 조회만 하는 사용자
        // ========================================
        // 대다수 사용자: 게시글 목록 조회 + 게시글 상세 조회
        lurker_scenario: {
            executor: 'ramping-vus',
            startTime: '10s',
            stages: [
                { duration: '30s', target: 700 },   // 빠른 증가
                { duration: '1m', target: 1400 },   // 피크 (70% of 2000)
                { duration: '1m', target: 1400 },   // 유지
                { duration: '30s', target: 0 },     // 종료
            ],
            exec: 'lurkerFlow',
            tags: { test_type: 'lurker', user_type: 'reader' },
        },

        // ========================================
        // 2️⃣ Active Reader Scenario (15%) - 조회 + 좋아요
        // ========================================
        active_reader_scenario: {
            executor: 'ramping-vus',
            startTime: '10s',
            stages: [
                { duration: '30s', target: 150 },
                { duration: '1m', target: 300 },    // 피크 (15% of 2000)
                { duration: '1m', target: 300 },
                { duration: '30s', target: 0 },
            ],
            exec: 'activeReaderFlow',
            tags: { test_type: 'active_reader', user_type: 'engaged' },
        },

        // ========================================
        // 3️⃣ Commenter Scenario (10%) - 조회 + 좋아요 + 댓글
        // ========================================
        commenter_scenario: {
            executor: 'ramping-vus',
            startTime: '10s',
            stages: [
                { duration: '30s', target: 100 },
                { duration: '1m', target: 200 },    // 피크 (10% of 2000)
                { duration: '1m', target: 200 },
                { duration: '30s', target: 0 },
            ],
            exec: 'commenterFlow',
            tags: { test_type: 'commenter', user_type: 'contributor' },
        },

        // ========================================
        // 4️⃣ Creator Scenario (5%) - 게시글 작성 포함 모든 활동
        // ========================================
        creator_scenario: {
            executor: 'ramping-vus',
            startTime: '10s',
            stages: [
                { duration: '30s', target: 50 },
                { duration: '1m', target: 100 },    // 피크 (5% of 2000)
                { duration: '1m', target: 100 },
                { duration: '30s', target: 0 },
            ],
            exec: 'creatorFlow',
            tags: { test_type: 'creator', user_type: 'content_creator' },
        },

        // ========================================
        // 5️⃣ Hot Post Stress - 인기 게시글 동시성 테스트
        // ========================================
        hot_post_stress: {
            executor: 'ramping-vus',
            startTime: '1m',
            stages: [
                { duration: '20s', target: 200 },
                { duration: '40s', target: 500 },   // 인기 게시글에 집중
                { duration: '20s', target: 0 },
            ],
            exec: 'hotPostStressFlow',
            tags: { test_type: 'stress', user_type: 'viral' },
        },
    },

    // ========================================
    // 📊 성능 목표 (DAU 30만 기준)
    // ========================================
    thresholds: {
        // ✅ HTTP 전체 응답 시간
        http_req_duration: ['p(95)<1500', 'p(99)<3000'],

        // ✅ HTTP 실패율
        http_req_failed: ['rate<0.05'],         // 5% 이하

        // ✅ 커스텀 에러율
        error_rate: ['rate<0.05'],

        // ✅ 동시성 에러
        concurrency_errors: ['count<50'],

        // ✅ 개별 작업 응답 시간
        post_view_duration: ['p(95)<1000'],
        like_toggle_duration: ['p(95)<500'],
        comment_creation_duration: ['p(95)<1500'],
        post_creation_duration: ['p(95)<2000'],
    },
};

// ==================== Setup & Teardown ====================
export function setup() {
    console.log('🚀 DAU 30만 Community Load Test');
    console.log('========================================');
    console.log(`📝 BASE_URL: ${BASE_URL}`);
    console.log(`📝 Post ID Range: ${MIN_POST_ID} ~ ${MAX_POST_ID}`);
    console.log(`📝 User ID Range: ${MIN_USER_ID} ~ ${MAX_USER_ID}`);
    console.log('========================================');
    console.log('📊 VU Distribution (Total 2000):');
    console.log('   - Lurker (70%): 1400 VU');
    console.log('   - Active Reader (15%): 300 VU');
    console.log('   - Commenter (10%): 200 VU');
    console.log('   - Creator (5%): 100 VU');
    console.log('========================================');

    // API 헬스 체크
    const res = http.get(`${API_BASE}/posts?page=0&size=1`);

    if (res.status !== 200) {
        console.error(`❌ API health check failed: ${res.status}`);
        throw new Error(`API health check failed: ${res.status}`);
    }

    console.log('✅ API health check passed');

    return {
        startTime: new Date().toISOString(),
        baseUrl: API_BASE,
    };
}

export function teardown(data) {
    console.log(`\n🏁 Test completed. Started at: ${data.startTime}`);
    console.log('📊 Check your monitoring dashboard for detailed metrics');
}

// ==================== Helper Functions ====================

function getHeaders() {
    return { 'Content-Type': 'application/json' };
}

function getMultipartHeaders() {
    return {};
}

/**
 * 🔥 각 VU에 고유한 User ID 할당
 */
function getUniqueUserId() {
    const vuId = __VU || 1;
    const userIdRange = MAX_USER_ID - MIN_USER_ID + 1;
    return MIN_USER_ID + (vuId % userIdRange);
}

/**
 * 🎲 랜덤 User ID (VU와 무관)
 */
function getRandomUserId() {
    return randomIntBetween(MIN_USER_ID, MAX_USER_ID);
}

/**
 * 🎲 랜덤 게시글 ID (파레토 법칙 적용)
 */
function getRandomPostId() {
    if (Math.random() < 0.8) {
        // Hot posts: 80% 확률로 인기 게시글 (상위 20%)
        const hotRange = Math.max(1, Math.floor((MAX_POST_ID - MIN_POST_ID + 1) * 0.2));
        return randomIntBetween(MIN_POST_ID, MIN_POST_ID + hotRange - 1);
    } else {
        return randomIntBetween(MIN_POST_ID, MAX_POST_ID);
    }
}

/**
 * 🔥 인기 게시글 ID (상위 10개)
 */
function getHotPostId() {
    const hotRange = Math.min(10, MAX_POST_ID - MIN_POST_ID + 1);
    return randomIntBetween(MIN_POST_ID, MIN_POST_ID + hotRange - 1);
}

/**
 * ✅ HTTP 응답 검증
 */
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
            }
        }
    } else {
        errorRate.add(0);
    }

    return success;
}

// ==================== API Functions ====================

/**
 * 📋 게시글 목록 조회
 */
function getPostList(page = 0) {
    const res = http.get(`${API_BASE}/posts?page=${page}&size=10`, {
        headers: getHeaders(),
        tags: { name: 'list_posts' },
    });
    checkResponse(res, { tag: 'ListPosts' });
    postListDuration.add(res.timings.duration);
    return res;
}

/**
 * 📖 게시글 상세 조회
 */
function getPost(postId) {
    const res = http.get(`${API_BASE}/posts/${postId}`, {
        headers: getHeaders(),
        tags: { name: 'get_post' },
    });
    checkResponse(res, { tag: 'GetPost', allowNotFound: true });
    postViewDuration.add(res.timings.duration);
    postViews.add(1);
    return res;
}

/**
 * 👁️ 조회수 증가
 */
function increaseViewCount(postId) {
    const res = http.post(`${API_BASE}/posts/${postId}/views`, null, {
        headers: getHeaders(),
        tags: { name: 'view_count' },
    });
    checkResponse(res, { tag: 'ViewCount' });
    viewCountDuration.add(res.timings.duration);
    viewCountOperations.add(1);
    return res;
}

/**
 * ❤️ 게시글 좋아요 토글
 */
function togglePostLike(postId, userId) {
    const res = http.post(`${API_BASE}/posts/${postId}/likes?testUserId=${userId}`, null, {
        headers: getHeaders(),
        tags: { name: 'post_like' },
    });

    check(res, {
        'like toggle success': (r) => r.status === 200 || r.status === 201,
    });

    likeToggleDuration.add(res.timings.duration);
    likeOperations.add(1);

    if (res.status >= 500) {
        errorRate.add(1);
    }

    return res;
}

/**
 * 💬 댓글 목록 조회
 */
function getComments(postId) {
    const res = http.get(`${API_BASE}/posts/${postId}/comments`, {
        headers: getHeaders(),
        tags: { name: 'get_comments' },
    });
    checkResponse(res, { tag: 'GetComments' });
    return res;
}

/**
 * 💬 댓글 작성
 */
function createComment(postId, userId) {
    const payload = JSON.stringify({
        content: `Test comment by user ${userId} at ${Date.now()}`
    });

    const res = http.post(`${API_BASE}/posts/${postId}/comments`, payload, {
        headers: getHeaders(),
        tags: { name: 'create_comment' },
    });

    checkResponse(res, { tag: 'CreateComment', expectStatus: 201 });
    commentCreationDuration.add(res.timings.duration);
    commentCreations.add(1);

    return res;
}

/**
 * ✍️ 게시글 작성
 */
function createPost(userId) {
    const categories = ['자유게시판', '질문', '정보공유', '공지'];

    const formData = {
        request: http.file(JSON.stringify({
            title: `Performance Test Post ${Date.now()}`,
            content: `This is a test post created during load testing at ${new Date().toISOString()}`,
            category: randomItem(categories)
        }), 'request.json', 'application/json')
    };

    const res = http.post(`${API_BASE}/posts`, formData, {
        headers: getMultipartHeaders(),
        tags: { name: 'create_post' },
    });

    checkResponse(res, { tag: 'CreatePost', expectStatus: 201 });
    postCreationDuration.add(res.timings.duration);
    postCreations.add(1);

    return res;
}

/**
 * ❤️ 댓글 좋아요 토글
 */
function toggleCommentLike(postId, commentId) {
    const res = http.post(`${API_BASE}/posts/${postId}/comments/${commentId}/likes`, null, {
        headers: getHeaders(),
        tags: { name: 'comment_like' },
    });

    check(res, {
        'comment like attempt': (r) => r.status === 200 || r.status === 201 || r.status === 401,
    });

    return res;
}

// ==================== Test Scenarios ====================

/**
 * 0️⃣ Smoke Test
 */
export function smokeTest() {
    group('Smoke Test', () => {
        getPostList(0);
        sleep(0.5);

        const postId = getRandomPostId();
        getPost(postId);
        sleep(0.5);
    });
}

/**
 * 1️⃣ Lurker Flow (70%)
 * 조회만 하는 사용자
 */
export function lurkerFlow() {
    activeVUs.add(1);

    group('Lurker - Browse & Read', () => {
        // 1. 게시글 목록 조회 (페이지 랜덤)
        const page = randomIntBetween(0, 10);
        getPostList(page);

        sleep(randomIntBetween(1, 3));

        // 2. 2-3개 게시글 상세 조회
        const numPosts = randomIntBetween(2, 3);
        for (let i = 0; i < numPosts; i++) {
            const postId = getRandomPostId();
            const res = getPost(postId);

            if (res.status === 200) {
                // 조회수 증가
                increaseViewCount(postId);

                // 50% 확률로 댓글도 조회
                if (Math.random() < 0.5) {
                    sleep(randomIntBetween(0.5, 1));
                    getComments(postId);
                }
            }

            sleep(randomIntBetween(2, 5));
        }
    });
}

/**
 * 2️⃣ Active Reader Flow (15%)
 * 조회 + 좋아요
 */
export function activeReaderFlow() {
    activeVUs.add(1);
    const userId = getUniqueUserId();

    group('Active Reader - Read & Like', () => {
        // 1. 게시글 목록 조회
        getPostList(randomIntBetween(0, 5));

        sleep(randomIntBetween(1, 2));

        // 2. 1-2개 게시글 상세 조회 + 좋아요
        const numPosts = randomIntBetween(1, 2);
        for (let i = 0; i < numPosts; i++) {
            const postId = getRandomPostId();
            const res = getPost(postId);

            if (res.status === 200) {
                increaseViewCount(postId);

                sleep(randomIntBetween(1, 3));

                // 댓글 조회
                getComments(postId);

                sleep(randomIntBetween(0.5, 1));

                // 70% 확률로 좋아요
                if (Math.random() < 0.7) {
                    togglePostLike(postId, userId);
                }
            }

            sleep(randomIntBetween(2, 4));
        }
    });
}

/**
 * 3️⃣ Commenter Flow (10%)
 * 조회 + 좋아요 + 댓글 작성
 */
export function commenterFlow() {
    activeVUs.add(1);
    const userId = getUniqueUserId();

    group('Commenter - Read, Like & Comment', () => {
        // 1. 게시글 목록 조회
        getPostList(randomIntBetween(0, 5));

        sleep(randomIntBetween(1, 2));

        // 2. 게시글 상세 조회
        const postId = getRandomPostId();
        const res = getPost(postId);

        if (res.status === 200) {
            increaseViewCount(postId);

            sleep(randomIntBetween(1, 2));

            // 댓글 조회
            const commentsRes = getComments(postId);

            sleep(randomIntBetween(1, 2));

            // 80% 확률로 좋아요
            if (Math.random() < 0.8) {
                togglePostLike(postId, userId);
            }

            sleep(randomIntBetween(0.5, 1));

            // 60% 확률로 댓글 작성
            if (Math.random() < 0.6) {
                createComment(postId, userId);
            }

            // 30% 확률로 기존 댓글에 좋아요
            if (Math.random() < 0.3 && commentsRes.status === 200) {
                try {
                    const comments = commentsRes.json();
                    if (Array.isArray(comments) && comments.length > 0) {
                        const randomComment = randomItem(comments);
                        if (randomComment && randomComment.commentId) {
                            toggleCommentLike(postId, randomComment.commentId);
                        }
                    }
                } catch (e) {
                    // JSON 파싱 실패 무시
                }
            }
        }

        sleep(randomIntBetween(3, 6));
    });
}

/**
 * 4️⃣ Creator Flow (5%)
 * 게시글 작성 + 모든 활동
 */
export function creatorFlow() {
    activeVUs.add(1);
    const userId = getUniqueUserId();

    group('Creator - Full Activity', () => {
        const action = Math.random();

        if (action < 0.4) {
            // 40%: 게시글 작성
            createPost(userId);
            sleep(randomIntBetween(3, 5));

        } else if (action < 0.7) {
            // 30%: 댓글 작성
            const postId = getRandomPostId();
            const res = getPost(postId);

            if (res.status === 200) {
                increaseViewCount(postId);
                sleep(randomIntBetween(1, 2));

                createComment(postId, userId);

                // 80% 확률로 좋아요도
                if (Math.random() < 0.8) {
                    togglePostLike(postId, userId);
                }
            }

            sleep(randomIntBetween(2, 4));

        } else {
            // 30%: 여러 게시글 조회 + 상호작용
            getPostList(0);
            sleep(randomIntBetween(0.5, 1));

            const numPosts = randomIntBetween(2, 4);
            for (let i = 0; i < numPosts; i++) {
                const postId = getRandomPostId();
                const res = getPost(postId);

                if (res.status === 200) {
                    increaseViewCount(postId);

                    if (Math.random() < 0.5) {
                        togglePostLike(postId, userId);
                    }
                }

                sleep(randomIntBetween(1, 2));
            }
        }
    });
}

/**
 * 5️⃣ Hot Post Stress Flow
 * 인기 게시글 동시성 테스트
 */
export function hotPostStressFlow() {
    activeVUs.add(1);
    const userId = getUniqueUserId();

    group('Hot Post Stress', () => {
        const hotPostId = getHotPostId();

        const action = Math.random();

        if (action < 0.4) {
            // 40%: 조회수 증가 (동시성 테스트)
            increaseViewCount(hotPostId);

        } else if (action < 0.8) {
            // 40%: 좋아요 토글 (동시성 테스트)
            togglePostLike(hotPostId, userId);

        } else {
            // 20%: 댓글 작성 (동시성 테스트)
            createComment(hotPostId, userId);
        }

        sleep(randomIntBetween(0.2, 0.5));  // 빠른 반복!
    });
}
