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

// ==================== Test Data Configuration ====================
// 🔧 실제 DB의 게시글 ID 범위로 설정 (존재하지 않는 ID 조회 방지)
const MIN_POST_ID = 248;
const MAX_POST_ID = parseInt(__ENV.MAX_POST_ID) || 4799;

// ==================== ⚡ QUICK TEST (3분) - DAU 30만 기준 빠른 검증 ====================
// 🎯 목적: 개발 중 빠른 피드백을 위한 짧은 성능 테스트
// ⏱️ 총 시간: 3분 (본 테스트의 30%)
//
// 📊 서비스 규모:
// - DAU: 300,000명
// - 피크 시간대 동시접속자: ~20,000명
// - 본 테스트 목표: 2,000 VU (피크의 10%)
// - Quick 테스트 목표: 500 VU (본 테스트의 25%, 빠른 검증용)
//
// 📊 사용자 비율 (본 테스트와 동일한 비율 유지):
// - Lurker (70%): 350 VU - 조회만
// - Active User (20%): 100 VU - 조회 + 간단한 상호작용
// - Interactive (8%): 40 VU - 적극적 상호작용
// - Creator (2%): 10 VU - 콘텐츠 생성
// - Total: 500 VU
//
// 📌 사용 시나리오:
// - 코드 수정 후 즉시 성능 확인
// - PR 생성 전 빠른 검증
// - 로컬/개발 환경에서 자주 실행
//
// 🚀 실행 방법:
//    k6 run k6/community/v3Quick.js
//
export const options = {
    scenarios: {
        // ========================================
        // 1️⃣ Smoke Test: API 기본 기능 검증
        // ========================================
        // 🎯 목적: 테스트 시작 전 API가 정상 작동하는지 확인
        // 📊 부하: 최소 (1 VU)
        // ⏱️ 시간: 30초
        smoke_test: {
            executor: 'constant-vus',
            vus: 1,
            duration: '30s',
            exec: 'smokeTest',
            tags: { test_type: 'smoke' },
        },

        // ========================================
        // 2️⃣ Lurker Scenario - 조회만 하는 사용자 (70%)
        // ========================================
        // 🎯 목적: 대다수 사용자의 브라우징 패턴 (빠른 검증)
        // 📊 부하: 최대 350 VU (전체의 70%)
        // ⏱️ 시간: 90초
        lurker_scenario: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 250 },   // 빠른 증가
                { duration: '30s', target: 350 },   // 피크
                { duration: '30s', target: 0 },     // 종료
            ],
            exec: 'lurkerFlow',
            tags: { test_type: 'lurker', user_type: 'reader' },
        },

        // ========================================
        // 3️⃣ Active User Scenario - 조회 + 간단한 상호작용 (20%)
        // ========================================
        // 🎯 목적: 콘텐츠 소비 + 가벼운 참여
        // 📊 부하: 최대 100 VU (전체의 20%)
        // ⏱️ 시간: 90초
        active_user_scenario: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 70 },
                { duration: '30s', target: 100 },   // 피크
                { duration: '30s', target: 0 },
            ],
            exec: 'activeUserFlow',
            tags: { test_type: 'active', user_type: 'engaged_reader' },
        },

        // ========================================
        // 4️⃣ Interactive Scenario - 적극적 상호작용 (8%)
        // ========================================
        // 🎯 목적: 좋아요 토글, 댓글 작성 등 활발한 상호작용 (동시성 테스트)
        // 📊 부하: 최대 40 VU (전체의 8%)
        // ⏱️ 시간: 90초
        interactive_scenario: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 30 },
                { duration: '30s', target: 40 },    // 피크 (동시성 압박)
                { duration: '30s', target: 0 },
            ],
            exec: 'interactiveFlow',
            tags: { test_type: 'interactive', user_type: 'contributor' },
        },

        // ========================================
        // 5️⃣ Creator Scenario - 콘텐츠 생성 (2%)
        // ========================================
        // 🎯 목적: 게시글/댓글 작성 등 쓰기 작업
        // 📊 부하: 최대 10 VU (전체의 2%)
        // ⏱️ 시간: 90초
        creator_scenario: {
            executor: 'ramping-vus',
            startTime: '30s',
            stages: [
                { duration: '30s', target: 7 },
                { duration: '30s', target: 10 },    // 피크
                { duration: '30s', target: 0 },
            ],
            exec: 'creatorFlow',
            tags: { test_type: 'creator', user_type: 'content_creator' },
        },

        // ========================================
        // 6️⃣ Spike Scenario - 바이럴 트래픽 급증
        // ========================================
        // 🎯 목적: 갑작스런 트래픽 폭증 시뮬레이션
        // 📊 부하: 추가 100 VU (피크 대비 +20% 급증)
        // ⏱️ 시간: 60초 (중간에 짧게 발생)
        spike_scenario: {
            executor: 'ramping-vus',
            startTime: '1m30s',                     // 중간에 갑자기 발생
            stages: [
                { duration: '10s', target: 100 },   // 급증!
                { duration: '30s', target: 100 },   // 유지
                { duration: '20s', target: 0 },     // 급감
            ],
            exec: 'spikeFlow',
            tags: { test_type: 'spike', user_type: 'viral_traffic' },
        },
    },

    // ========================================
    // 📊 성능 목표 (완화된 기준 - Quick Test)
    // ========================================
    // ⚠️ Quick 테스트는 빠른 피드백을 위해 기준이 완화됨
    //
    // 💡 본 테스트 vs Quick 테스트:
    //    - 본 테스트: p(95)<1500ms, 실패율 5%
    //    - Quick 테스트: p(95)<2000ms, 실패율 10% (더 관대한 기준)
    //
    thresholds: {
        // ✅ HTTP 전체 응답 시간
        http_req_duration: ['p(95)<2000'],     // 95% 요청: 2초 이내 (본 테스트보다 완화)

        // ✅ HTTP 실패율
        http_req_failed: ['rate<0.1'],         // 10% 이하 (빠른 테스트라 완화)

        // ✅ 커스텀 에러율
        error_rate: ['rate<0.1'],              // 10% 이하

        // ✅ 동시성 에러
        concurrency_errors: ['count<100'],     // 100건 이하 (본 테스트: 50건)
    },
};

// ==================== Setup & Teardown ====================
/**
 * 🔧 Setup: 테스트 시작 전 초기화
 * - API 헬스 체크로 서버가 정상 작동하는지 확인
 * - 실패 시 테스트 중단
 */
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

/**
 * 🔧 Teardown: 테스트 종료 후 정리
 *
 * ⚠️ k6는 DB에 직접 접근할 수 없어서 자동 정리 불가능
 *
 * 🧹 테스트 데이터 정리 방법:
 *    테스트 종료 후 아래 명령어 실행:
 *
 *    ./k6/community/cleanup-test-data.sh "시작시간"
 *
 * 💡 자동화된 실행 예시:
 *    START_TIME=$(date -u '+%Y-%m-%d %H:%M:%S')
 *    k6 run k6/community/v3Quick.js
 *    ./k6/community/cleanup-test-data.sh "$START_TIME"
 *
 * 📝 스크립트가 하는 일:
 *    - "Quick Test Post", "Test Post"로 시작하는 게시글 삭제
 *    - "test comment"를 포함한 댓글 삭제
 *    - CASCADE로 관련 좋아요 자동 삭제
 *    - 기존 프로덕션 데이터는 보존
 */
export function teardown(data) {
    console.log(`⚡ Quick Test completed. Started at: ${data.startTime}`);
    console.log(`🧹 To cleanup test data, run:`);
    console.log(`   ./k6/community/cleanup-test-data.sh "${data.startTime}"`);
}

// ==================== Helper Functions ====================
/**
 * 📝 JSON 요청용 헤더 생성
 */
function getHeaders() {
    return { 'Content-Type': 'application/json' };
}

/**
 * 📝 Multipart/form-data 요청용 헤더 생성
 * (k6가 자동으로 boundary 설정)
 */
function getMultipartHeaders() {
    return {};
}

/**
 * ✅ HTTP 응답 검증 및 메트릭 기록
 *
 * @param {Object} res - HTTP 응답 객체
 * @param {Object} options - 검증 옵션
 *   - tag: 로그용 태그
 *   - expectStatus: 예상 상태 코드 (기본: 200)
 *   - allowNotFound: 404도 성공으로 간주 (기본: false)
 *
 * 동작:
 * 1. 응답 시간 체크 (3초 이내)
 * 2. 상태 코드 검증
 * 3. 5xx 에러 발생 시 메트릭 기록 및 동시성 이슈 감지
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

    // 5xx 에러만 실제 에러로 카운팅
    if (res.status >= 500) {
        errorRate.add(1);
        dbErrors.add(1);

        // 동시성 이슈 감지
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

/**
 * 🎲 랜덤 게시글 ID 선택 (파레토 법칙 적용)
 *
 * 💡 80% 확률로 인기 게시글(1-200번), 20% 확률로 전체 게시글
 *
 * @returns {number} 게시글 ID
 */
function getRandomPostId() {
    if (Math.random() < 0.8) {
        // Hot posts: 전체 요청의 80%
        return randomIntBetween(MIN_POST_ID, Math.min(200, MAX_POST_ID));
    } else {
        // All posts: 나머지 20%
        return randomIntBetween(MIN_POST_ID, MAX_POST_ID);
    }
}

/**
 * ✍️ 게시글 작성 API 호출
 *
 * API: POST /api/v1/community/posts
 *
 * @returns {number|null} 생성된 게시글 ID (실패 시 null)
 */
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

/**
 * 💬 댓글 작성 API 호출
 *
 * API: POST /api/v1/community/posts/{postId}/comments
 *
 * @param {number} postId - 댓글을 작성할 게시글 ID
 * @returns {boolean} 성공 여부
 */
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
 * ❤️ 좋아요 토글 헬퍼 함수
 *
 * API: POST /api/v1/community/posts/{postId}/likes
 *
 * 💡 70% 확률로 1회, 30% 확률로 2-3회 토글 (현실적인 사용자 행동)
 *
 * @param {number} postId - 게시글 ID
 * @param {object} options - { tag, allowMultiple }
 */
function toggleLike(postId, options = {}) {
    const { tag = 'like', allowMultiple = true } = options;

    // 🔥 핵심: 각 가상 유저마다 다른 userId 사용 (좋아요 누적을 위해)
    const userId = randomIntBetween(2, 100);

    // 70% 확률로 1회, 30% 확률로 2-3회 토글
    const shouldToggleMultiple = allowMultiple && Math.random() < 0.3;
    const toggleCount = shouldToggleMultiple ? randomIntBetween(2, 3) : 1;

    for (let i = 0; i < toggleCount; i++) {
        const res = http.post(`${API_BASE}/posts/${postId}/likes?testUserId=${userId}`, null, {
            headers: getHeaders(),
            tags: { name: tag },
        });
        checkResponse(res, { tag: 'Like' });

        // 여러 번 토글할 때 사이에 짧은 대기 (0.1~0.3초)
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
