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
const MAX_POST_ID = parseInt(__ENV.MAX_POST_ID) || 1394;  // 실제 DB 게시글 수에 맞춤

// ==================== Test Scenarios (현실적인 혼합 워크로드) ====================
// 총 테스트 시간: 10분
//
// 🎯 핵심 개선사항:
// 1. ✅ 사용자 페르소나별 시나리오 분리 (조회, 상호작용, 작성)
// 2. ✅ 모든 작업이 동시에 실행 (현실적인 혼합 워크로드)
// 3. ✅ Sleep 최소화 및 더 빠른 반복
// 4. ✅ 실제 사용자 비율 반영 (70% 조회, 20% 상호작용, 10% 작성)
//
// 📌 테스트 시나리오 설명:
// 1. Smoke Test (1분): 기본 기능 검증
// 2. 혼합 워크로드 (9분): 모든 사용자 유형이 동시에 활동
//    - Lurker (70%): 조회만
//    - Active User (20%): 조회 + 좋아요 + 댓글 조회
//    - Interactive User (8%): 좋아요 토글 집중
//    - Creator (2%): 게시글/댓글 작성
//
// 💡 VU (Virtual User): 가상의 동시 접속 사용자 수
//    - 1 VU = 1명의 유저가 계속 요청을 보내는 것
//    - 100 VU = 100명이 동시에 API 호출
//
export const options = {
    scenarios: {
        // ========================================
        // 1️⃣ Smoke Test: 기본 기능 검증
        // ========================================
        // 목적: 배포 전 기본 API들이 정상 작동하는지 확인
        // 부하: 최소 (1명의 유저)
        // 시간: 1분
        smoke_test: {
            executor: 'constant-vus',
            vus: 1,
            duration: '1m',
            exec: 'smokeTest',
            tags: { test_type: 'smoke' },
        },

        // ========================================
        // 2️⃣ Lurker Scenario (70% - 조회만 하는 사용자)
        // ========================================
        // 💡 현실 반영:
        //    - 대부분의 사용자는 그냥 둘러보기만 함
        //    - 목록 조회 → 게시글 조회 → 다른 게시글 조회
        //    - 좋아요나 댓글 없이 빠르게 이동
        //
        // 부하: 높음 (전체의 70%)
        // 시간: 9분 (smoke 후 동시 시작)
        lurker_scenario: {
            executor: 'ramping-vus',
            startTime: '1m',
            stages: [
                { duration: '1m', target: 50 },    // 워밍업
                { duration: '2m', target: 100 },   // 일반 트래픽
                { duration: '2m', target: 150 },   // 피크 타임
                { duration: '2m', target: 200 },   // 최대 부하
                { duration: '1m', target: 100 },   // 쿨다운
                { duration: '1m', target: 0 },     // 종료
            ],
            exec: 'lurkerFlow',
            tags: { test_type: 'lurker', user_type: 'reader' },
        },

        // ========================================
        // 3️⃣ Active User Scenario (20% - 조회 + 간단한 상호작용)
        // ========================================
        // 💡 현실 반영:
        //    - 조회하면서 가끔 좋아요 누르고
        //    - 댓글 읽고
        //    - 빠르게 여러 게시글 순회
        //
        // 부하: 중간 (전체의 20%)
        active_user_scenario: {
            executor: 'ramping-vus',
            startTime: '1m',
            stages: [
                { duration: '1m', target: 15 },
                { duration: '2m', target: 30 },
                { duration: '2m', target: 45 },
                { duration: '2m', target: 60 },
                { duration: '1m', target: 30 },
                { duration: '1m', target: 0 },
            ],
            exec: 'activeUserFlow',
            tags: { test_type: 'active', user_type: 'engaged_reader' },
        },

        // ========================================
        // 4️⃣ Interactive Scenario (8% - 좋아요/댓글 상호작용 집중)
        // ========================================
        // 💡 현실 반영:
        //    - 인기 게시글에 좋아요 토글 (동시성 테스트)
        //    - 댓글 작성
        //    - 빠른 반복 (동시성 압박)
        //
        // 부하: 동시성 압박 (전체의 8%)
        interactive_scenario: {
            executor: 'ramping-vus',
            startTime: '1m',
            stages: [
                { duration: '1m', target: 10 },
                { duration: '2m', target: 20 },
                { duration: '2m', target: 30 },
                { duration: '2m', target: 50 },    // 동시성 압박!
                { duration: '1m', target: 20 },
                { duration: '1m', target: 0 },
            ],
            exec: 'interactiveFlow',
            tags: { test_type: 'interactive', user_type: 'contributor' },
        },

        // ========================================
        // 5️⃣ Creator Scenario (2% - 게시글/댓글 작성)
        // ========================================
        // 💡 현실 반영:
        //    - 소수의 사용자만 게시글 작성
        //    - 댓글 작성
        //    - 느린 작업 (이미지 업로드 포함 가능)
        //
        // 부하: 낮음 (전체의 2%)
        creator_scenario: {
            executor: 'ramping-vus',
            startTime: '1m',
            stages: [
                { duration: '1m', target: 2 },
                { duration: '2m', target: 5 },
                { duration: '2m', target: 8 },
                { duration: '2m', target: 10 },
                { duration: '1m', target: 5 },
                { duration: '1m', target: 0 },
            ],
            exec: 'creatorFlow',
            tags: { test_type: 'creator', user_type: 'content_creator' },
        },

        // ========================================
        // 6️⃣ Spike Scenario (트래픽 급증)
        // ========================================
        // 💡 현실 반영:
        //    - 바이럴 게시글로 갑작스런 트래픽 급증
        //    - 짧은 시간에 많은 조회와 좋아요
        //
        // 부하: 극도로 높음
        // 시간: 2분 (중간에 짧게 실행)
        spike_scenario: {
            executor: 'ramping-vus',
            startTime: '5m',    // 중간에 갑자기 발생
            stages: [
                { duration: '20s', target: 300 },  // 급증!
                { duration: '1m', target: 300 },   // 유지
                { duration: '40s', target: 0 },    // 급감
            ],
            exec: 'spikeFlow',
            tags: { test_type: 'spike', user_type: 'viral_traffic' },
        },
    },

    // ========================================
    // 📊 성능 목표 (Thresholds)
    // ========================================
    // 이 기준을 하나라도 통과 못하면 테스트 실패!
    //
    // 💡 Percentile(백분위수) 설명:
    //    - p(95) < 1500ms: 전체 요청의 95%가 1.5초 이내 응답
    //    - p(99) < 3000ms: 전체 요청의 99%가 3초 이내 응답
    //    → 상위 5%는 느려도 OK, 하지만 1.5초는 넘지 말자!
    thresholds: {
        // HTTP 응답 시간 (매우 중요!)
        http_req_duration: [
            'p(95)<1500',   // 95%의 요청이 1.5초 이내 (사용자 체감 기준)
            'p(99)<3000'    // 99%의 요청이 3초 이내 (느린 요청도 3초 안에)
        ],

        // HTTP 실패율 (매우 중요!)
        http_req_failed: ['rate<0.05'],  // 실패율 5% 이하 (100개 중 5개까지 실패 허용)

        // 커스텀 에러율
        error_rate: ['rate<0.05'],       // 전체 에러율 5% 이하

        // API별 응답 시간 목표
        post_list_duration: ['p(95)<800'],         // 게시글 목록: 0.8초 이내
        post_creation_duration: ['p(95)<2000'],    // 게시글 작성: 2초 이내 (이미지 업로드 포함)
        comment_creation_duration: ['p(95)<1500'], // 댓글 작성: 1.5초 이내

        // 동시성 에러 (데이터 정합성 검증)
        concurrency_errors: ['count<50'],  // 동시성 에러 50건 이하 (Deadlock, Race Condition 등)
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

// ==================== Helper Functions ====================

/**
 * 📌 랜덤 게시글 ID 선택 (파레토 법칙 적용)
 *
 * 💡 파레토 법칙 (80:20 법칙):
 *    - 실제 서비스에서 전체 게시글의 20%가 전체 트래픽의 80%를 차지함
 *    - 예: 1-200번 게시글(인기글)이 80%의 조회수를 받음
 *
 * 구현:
 *    - 80% 확률: 1-200번 게시글 중 랜덤 선택 (Hot Posts)
 *    - 20% 확률: 1-1394번 전체 게시글 중 랜덤 선택 (Long-tail)
 */
function getRandomPostId() {
    if (Math.random() < 0.8) {
        // Hot posts (상위 20%): 전체 요청의 80%
        return randomIntBetween(MIN_POST_ID, Math.min(200, MAX_POST_ID));
    } else {
        // All posts (전체): 전체 요청의 20%
        return randomIntBetween(MIN_POST_ID, MAX_POST_ID);
    }
}

/**
 * 📌 게시글 작성 API 호출
 *
 * 동작:
 *    1. 랜덤 카테고리 선택 (자유게시판, 질문, 정보공유, 공지)
 *    2. POST /api/v1/community/posts 호출
 *    3. 응답 시간을 post_creation_duration 메트릭에 기록
 *    4. 성공 시 생성된 postId 반환
 *
 * 💡 이미지 없이 JSON만 전송 (빠른 테스트를 위해)
 */
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
        tags: { name: 'create_post' },  // Grafana에서 API별로 필터링 가능
    });

    checkResponse(res, { tag: 'CreatePost', expectStatus: 201 });
    postCreationDuration.add(res.timings.duration);  // 커스텀 메트릭에 응답시간 기록

    if (res.status === 201 && res.json('postId')) {
        return res.json('postId');
    }
    return null;
}

/**
 * 📌 댓글 작성 API 호출
 *
 * 동작:
 *    1. POST /api/v1/community/posts/{postId}/comments 호출
 *    2. 응답 시간을 comment_creation_duration 메트릭에 기록
 *    3. 성공 여부 반환
 */
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
 * 1️⃣ Smoke Test: 기본 기능 검증
 *
 * 💡 목적:
 *    배포 전 핵심 API들이 정상 작동하는지 빠르게 확인
 *    - CI/CD 파이프라인에서 자동 실행
 *    - 프로덕션 배포 직전 마지막 체크
 *
 * 🔍 테스트 시나리오:
 *    1. 게시글 목록 조회 (GET /posts)
 *    2. 게시글 상세 조회 (GET /posts/{postId})
 *    3. 댓글 목록 조회 (GET /posts/{postId}/comments)
 *    4. 게시글 작성 (POST /posts)
 *    5. 댓글 작성 (POST /posts/{postId}/comments)
 *
 * ⏱️ 실행 시간: 1분 (1 VU)
 */
export function smokeTest() {
    group('Smoke Test - Basic Functionality', () => {
        // 1️⃣ 게시글 목록 조회 (페이지네이션)
        let res = http.get(`${API_BASE}/posts?page=0&size=10`, {
            headers: getHeaders(),
            tags: { name: 'list_posts' },
        });
        checkResponse(res, { tag: 'ListPosts' });
        postListDuration.add(res.timings.duration);
        sleep(1);  // 사용자가 목록을 보는 시간 (1초)

        // 2️⃣ 게시글 상세 조회
        const postId = getRandomPostId();
        res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'get_post' },
        });
        checkResponse(res, { tag: 'GetPost', allowNotFound: true });
        sleep(1);

        // 3️⃣ 댓글 목록 조회 (게시글이 존재할 때만)
        if (res.status === 200) {
            res = http.get(`${API_BASE}/posts/${postId}/comments`, {
                headers: getHeaders(),
                tags: { name: 'get_comments' },
            });
            checkResponse(res, { tag: 'GetComments' });
        }
        sleep(1);

        // 4️⃣ 게시글 작성
        createPost();
        sleep(2);  // 게시글 작성 후 잠시 대기

        // 5️⃣ 댓글 작성 (게시글이 존재할 때만)
        if (res.status === 200) {
            createComment(postId);
        }
        sleep(1);
    });
}

/**
 * 2️⃣ Lurker Flow: 조회만 하는 사용자 (70%)
 *
 * 💡 목적:
 *    대부분의 사용자는 그냥 둘러보기만 함
 *    - 빠르게 목록 조회
 *    - 게시글 클릭
 *    - 읽고 빠르게 다른 곳으로 이동
 *
 * ⏱️ Sleep 최소화: 0.5~2초
 */
export function lurkerFlow() {
    activeVUs.add(1);

    group('Lurker - Browse Only', () => {
        // 목록 조회
        let res = http.get(`${API_BASE}/posts?page=${randomIntBetween(0, 3)}&size=10`, {
            headers: getHeaders(),
            tags: { name: 'lurker_browse' },
        });
        checkResponse(res, { tag: 'LurkerBrowse' });
        postListDuration.add(res.timings.duration);

        sleep(randomIntBetween(0.5, 1.5));  // 빠른 스크롤

        // 게시글 조회 (1-2개)
        const numPosts = randomIntBetween(1, 2);
        for (let i = 0; i < numPosts; i++) {
            const postId = getRandomPostId();
            res = http.get(`${API_BASE}/posts/${postId}`, {
                headers: getHeaders(),
                tags: { name: 'lurker_read' },
            });
            checkResponse(res, { tag: 'LurkerRead', allowNotFound: true });

            sleep(randomIntBetween(0.5, 2));  // 빠르게 읽고 이동
        }
    });
}

/**
 * 3️⃣ Active User Flow: 조회 + 간단한 상호작용 (20%)
 *
 * 💡 목적:
 *    - 게시글 조회
 *    - 댓글 읽기
 *    - 가끔 좋아요
 *
 * ⏱️ Sleep 최소화: 1~3초
 */
export function activeUserFlow() {
    activeVUs.add(1);

    group('Active User - Read & React', () => {
        // 게시글 조회
        const postId = getRandomPostId();
        let res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'active_read' },
        });
        checkResponse(res, { tag: 'ActiveRead', allowNotFound: true });

        if (res.status === 200) {
            sleep(randomIntBetween(1, 2));  // 읽는 시간

            // 댓글 조회
            res = http.get(`${API_BASE}/posts/${postId}/comments`, {
                headers: getHeaders(),
                tags: { name: 'active_read_comments' },
            });
            checkResponse(res, { tag: 'ActiveReadComments' });

            sleep(randomIntBetween(0.5, 1.5));

            // 50% 확률로 좋아요 (70%는 한번만, 30%는 2-3번 토글)
            if (Math.random() < 0.5) {
                toggleLike(postId, { tag: 'active_like', allowMultiple: true });
                sleep(0.5);
            }
        } else {
            sleep(randomIntBetween(1, 2));
        }
    });
}

/**
 * 4️⃣ Interactive Flow: 좋아요/댓글 상호작용 집중 (8%)
 *
 * 💡 목적:
 *    - 인기 게시글에 좋아요 토글 (동시성 테스트!)
 *    - 댓글 작성
 *    - 빠른 반복으로 동시성 압박
 *
 * ⏱️ Sleep 최소화: 0.5~1초 (동시성 압박)
 */
export function interactiveFlow() {
    group('Interactive - Like & Comment', () => {
        // 인기 게시글 타겟 (1-10번) - 동시성 테스트
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

        sleep(randomIntBetween(0.5, 1));  // 빠른 반복
    });
}

/**
 * 5️⃣ Creator Flow: 게시글/댓글 작성 (2%)
 *
 * 💡 목적:
 *    - 게시글 작성
 *    - 댓글 작성
 *
 * ⏱️ Sleep: 2~4초 (작성 시간)
 */
export function creatorFlow() {
    group('Creator - Write Content', () => {
        const action = Math.random();

        if (action < 0.7) {
            // 70%: 게시글 작성
            createPost();
            sleep(randomIntBetween(2, 4));
        } else {
            // 30%: 댓글 작성
            const postId = getRandomPostId();
            createComment(postId);
            sleep(randomIntBetween(1, 2));
        }
    });
}

/**
 * 6️⃣ Spike Flow: 급격한 트래픽 증가 (바이럴)
 *
 * 💡 목적:
 *    - 바이럴 게시글로 트래픽 급증
 *    - 빠른 조회와 좋아요
 *
 * ⏱️ Sleep 최소화: 0.3~1초
 */
export function spikeFlow() {
    group('Spike - Viral Traffic', () => {
        const postId = getRandomPostId();

        // 빠른 조회
        const res = http.get(`${API_BASE}/posts/${postId}`, {
            headers: getHeaders(),
            tags: { name: 'spike_read' },
        });
        checkResponse(res, { tag: 'SpikeRead', allowNotFound: true });

        // 70% 확률로 좋아요 (바이럴 시 좋아요 폭증, 일부는 토글)
        if (res.status === 200 && Math.random() < 0.7) {
            toggleLike(postId, { tag: 'spike_like', allowMultiple: true });
        }

        sleep(randomIntBetween(0.3, 1));  // 매우 빠른 연속 액션
    });
}
