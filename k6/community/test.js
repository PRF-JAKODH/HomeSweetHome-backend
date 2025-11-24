import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

/**
 *
 */

// ==================== 📊 메트릭 설정 ====================
const deadlockCounter = new Counter('deadlock_count');
const errorRate = new Rate('error_rate');
const reqDuration = new Trend('req_duration');

// ==================== ⚙️ 테스트 설정 ====================
export const options = {
    scenarios: {
        viral_spike: {
            executor: 'ramping-vus', // 점진적 사용자 증가
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },   // 10초 동안 50명 진입 (워밍업)
                { duration: '20s', target: 300 },  // 30초 시점에 300명 도달 (부하 급증)
                { duration: '20s', target: 300 },  // 20초간 피크 유지 (데드락 유발 구간)
                { duration: '10s', target: 0 },    // 10초 동안 빠져나감
            ],
            exec: 'viralAction',
        },
    },
    thresholds: {
        // 데드락은 0건이어야 함
        deadlock_count: ['count==0'],
        // 에러율은 1% 미만
        error_rate: ['rate<0.01'],
        // 95%의 요청은 500ms 이내여야 함 (성능 기준)
        req_duration: ['p(95)<500'],
    },
};

// ==================== 🔧 기본 설정 ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/community`;
const TARGET_POST_ID = 1; // 모든 유저가 1번 글만 공격함 (경합 유도)

// 헤더 생성 (JSON)
const getHeaders = () => ({ 'Content-Type': 'application/json' });

// ==================== 🎬 메인 시나리오 ====================
export function viralAction() {
    const rand = Math.random();
    let res;
    let actionType = '';

    /**
     *
     */

    if (rand < 0.6) {
        // 조회수 증가
        actionType = 'VIEW';
        res = http.post(`${API_BASE}/posts/${TARGET_POST_ID}/views`, null, { headers: getHeaders() });
    }
    else if (rand < 0.8) {
        // 좋아요 토글
        actionType = 'LIKE';
        res = http.post(`${API_BASE}/posts/${TARGET_POST_ID}/likes`, null, { headers: getHeaders() });
    }
    else {
        // 댓글 작성
        actionType = 'COMMENT';
        const payload = JSON.stringify({ content: "데드락 유발용 댓글입니다." });
        res = http.post(`${API_BASE}/posts/${TARGET_POST_ID}/comments`, payload, { headers: getHeaders() });
    }

    // ==================== 결과 검증 ====================
    const success = check(res, {
        'Status 200 OK': (r) => r.status === 200 || r.status === 201,
    });

    // 에러 처리 및 데드락 감지
    if (!success) {
        errorRate.add(1);
        const body = res.body ? String(res.body) : "";

        // 응답 본문에 "Deadlock" 단어가 포함되어 있는지 확인
        if (res.status === 500 && body.includes('Deadlock')) {
            deadlockCounter.add(1);
            console.log(`Deadlock 발생! Action: ${actionType}`);
        }
    }

    // 성능 측정
    reqDuration.add(res.timings.duration);

    // Real User처럼 아주 짧은 대기 (0.1 ~ 0.5초)
    sleep(Math.random() * 0.4 + 0.1);
}

// ==================== 📋 결과 리포트 (가시성 UP) ====================
export function handleSummary(data) {
    const totalReqs = data.metrics.http_reqs.values.count;
    const deadlocks = data.metrics.deadlock_count ? data.metrics.deadlock_count.values.count : 0;
    const p95 = data.metrics.req_duration.values['p(95)'].toFixed(2);
    const failRate = (data.metrics.error_rate.values.rate * 100).toFixed(2);

    return {
        'stdout': `
    트래픽 요약
    ----------------------------------------------------------
    최대 동시 접속자 : 300 VUs
    총 요청 수       : ${totalReqs} 건
    평균 응답(P95)   : ${p95} ms

    `,
    };
}