import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

/**
 * 🔥 커뮤니티 High Load & Deadlock Stress Test
 *
 * 시나리오 구성:
 * 1. Baseline: 평상시 트래픽 (배경 노이즈)
 * 2. Viral Post: 특정 게시글(ID:1)에 수천 명이 동시에 조회/좋아요 (데드락 유발 구간)
 * 3. Peak Traffic: 전체 사용자가 급증하는 상황 (점심시간, 퇴근시간)
 */

// ==================== 커스텀 메트릭 ====================
const errorRate = new Rate('error_rate'); // 전체 에러율 통합
const deadlockCounter = new Counter('deadlock_errors'); // 데드락 의심 에러 카운트

const apiSuccessRate = new Rate('api_success_rate');
const postReadDuration = new Trend('post_read_duration');
const likeDuration = new Trend('like_duration');
const viewDuration = new Trend('view_duration');

// ==================== 테스트 설정 (현업 수준 상향 조정) ====================
export const options = {
    scenarios: {
        // 1. [Baseline] 평상시 배경 트래픽 (꾸준히 들어오는 요청)
        // VUs: 50명 지속
        background_traffic: {
            executor: 'constant-vus',
            vus: 50,
            duration: '5m', // 5분간 지속
            exec: 'testUserJourney',
        },

        // 2. [Critical] "인기글 선정" 상황 - 조회수 폭증 (Atomic Update 검증용)
        // 1번 게시글에 300명이 동시에 조회수 증가 요청 난사
        // 기존 JPA Dirty Checking 사용 시 100% 사망하는 구간
        viral_post_views: {
            executor: 'per-vu-iterations',
            vus: 300, // 동시 접속 300명
            iterations: 50, // 1인당 50번 클릭 (총 15,000 요청)
            maxDuration: '2m',
            startTime: '30s', // 시작 30초 후 돌입
            exec: 'testConcurrentViews',
        },

        // 3. [Critical] "선착순 이벤트/좋아요" 상황 - 좋아요 폭증 (데드락 유발 최적)
        // 1번 게시글에 200명이 동시에 좋아요 토글
        // S-Lock vs X-Lock 경합을 유도함
        viral_post_likes: {
            executor: 'per-vu-iterations',
            vus: 200,
            iterations: 20, // 1인당 20번 토글 (총 4,000 요청)
            maxDuration: '2m',
            startTime: '1m', // 시작 1분 후 돌입 (조회수 부하와 겹치게 함)
            exec: 'testConcurrentLikes',
        },

        // 4. [Stress] 점진적 부하 증가 (Breaking Point 탐색)
        // 0명 -> 1000명 -> 0명
        // 서버가 어디까지 버티는지 확인
        stress_ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 200 },  // 워밍업
                { duration: '2m', target: 1000 }, // 피크 부하 (천 명 동시 접속)
                { duration: '1m', target: 1000 }, // 피크 유지
                { duration: '1m', target: 0 },    // 쿨다운
            ],
            startTime: '2m', // 위 시나리오들과 겹치며 피날레 장식
            exec: 'testStress',
        },
    },

    thresholds: {
        // 전체 에러율 1% 미만이어야 통과
        error_rate: ['rate<0.01'],
        http_req_failed: ['rate<0.01'],

        // 데드락 에러는 단 1건도 허용하지 않음 (목표)
        deadlock_errors: ['count==0'],

        // 성능 목표 (현업 기준: 95% 요청이 500ms 이내 처리)
        http_req_duration: ['p(95)<500'],
        like_duration: ['p(95)<300'], // 좋아요는 가벼운 쿼리라 더 빨라야 함
    },
};

// ==================== 설정 ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/community`;
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const TARGET_POST_ID = 1; // 데드락 유발을 위해 1번 게시글 집중 공격

const getHeaders = () => {
    const headers = { 'Content-Type': 'application/json' };
    if (AUTH_TOKEN) headers['Authorization'] = `Bearer ${AUTH_TOKEN}`;
    return headers;
};

// ==================== 시나리오 함수 ====================

// 1. 사용자 여정 (배경 트래픽)
export function testUserJourney() {
    // 목록 조회 (가벼운 부하)
    const resList = http.get(`${API_BASE}/posts?page=0&size=10`, { headers: getHeaders(), tags: { name: 'List' } });
    check(resList, { 'Status 200': (r) => r.status === 200 }) || errorRate.add(1);

    sleep(Math.random() * 2 + 1); // 1~3초 생각 시간 (Real User)
}

// 2. 동시 조회수 증가 (데드락/갱신손실 테스트 핵심)
export function testConcurrentViews() {
    const res = http.post(
        `${API_BASE}/posts/${TARGET_POST_ID}/views`,
        null,
        { headers: getHeaders(), tags: { name: 'ViralView' } }
    );

    const success = check(res, {
        'Views 200 OK': (r) => r.status === 200,
    });

    if (!success) {
        errorRate.add(1);
        // 500 에러이고 내용에 Deadlock이 있으면 카운트
        if (res.status === 500 && res.body && res.body.includes('Deadlock')) {
            deadlockCounter.add(1);
            console.error('💀 Deadlock Detected in Views!');
        }
    }

    viewDuration.add(res.timings.duration);
    // sleep 없음: 기계적인 연타를 위해 쉼 없이 요청 (Atomic Update 검증)
}

// 3. 동시 좋아요 토글 (Lock Contention 테스트 핵심)
export function testConcurrentLikes() {
    const res = http.post(
        `${API_BASE}/posts/${TARGET_POST_ID}/likes`,
        null,
        { headers: getHeaders(), tags: { name: 'ViralLike' } }
    );

    const success = check(res, {
        'Likes 200 OK': (r) => r.status === 200,
    });

    if (!success) {
        errorRate.add(1);
        if (res.status === 500 && res.body && res.body.includes('Deadlock')) {
            deadlockCounter.add(1);
            console.error('💀 Deadlock Detected in Likes!');
        }
    }

    likeDuration.add(res.timings.duration);
    sleep(0.1); // 아주 짧은 간격
}

// 4. 스트레스 테스트 (랜덤 액션)
export function testStress() {
    const rand = Math.random();
    const headers = getHeaders();

    if (rand < 0.4) { // 40% 조회 (읽기 부하)
        http.get(`${API_BASE}/posts/${TARGET_POST_ID}`, { headers, tags: { name: 'StressRead' } });
    } else if (rand < 0.7) { // 30% 조회수 증가 (쓰기 부하)
        http.post(`${API_BASE}/posts/${TARGET_POST_ID}/views`, null, { headers, tags: { name: 'StressView' } });
    } else if (rand < 0.9) { // 20% 좋아요 (쓰기 부하)
        http.post(`${API_BASE}/posts/${TARGET_POST_ID}/likes`, null, { headers, tags: { name: 'StressLike' } });
    } else { // 10% 댓글 작성 (무거운 쓰기 부하)
        const payload = JSON.stringify({ content: "부하 테스트 댓글입니다." });
        http.post(`${API_BASE}/posts/${TARGET_POST_ID}/comments`, payload, { headers, tags: { name: 'StressComment' } });
    }

    sleep(Math.random() * 0.5); // 짧은 대기 시간 (0 ~ 0.5초)
}

// ==================== 리포트 생성 ====================
export function handleSummary(data) {
    return {
        'stdout': textSummary(data),
    };
}

function textSummary(data) {
    const totalReqs = data.metrics.http_reqs.values.count;
    const failedReqs = data.metrics.http_req_failed.values.count;
    const p95 = data.metrics.http_req_duration.values['p(95)'].toFixed(2);
    const deadlockCount = data.metrics.deadlock_errors ? data.metrics.deadlock_errors.values.count : 0;

    return `
    ================================================
    🔥 커뮤니티 서비스 High Load Test Report 🔥
    ================================================
    ✅ 총 요청 수      : ${totalReqs}
    ❌ 실패 요청 수    : ${failedReqs}
    💀 데드락 발생 수  : ${deadlockCount} (0이어야 함)
    
    ⏱️ 평균 응답 속도 (P95) : ${p95} ms
    ================================================
    ${deadlockCount > 0 ? "⛔ 경고: 데드락이 발생했습니다! 로직 수정이 필요합니다." : "🎉 축하합니다! 고부하 상황을 견뎌냈습니다."}
    ================================================
    `;
}