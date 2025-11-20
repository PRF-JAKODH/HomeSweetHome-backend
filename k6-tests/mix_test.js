import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

/**
 * 🧨 Deadlock Mix Test
 * 시나리오: 5가지 액션이 동시에 하나의 게시글(ID:1)을 수정하려고 시도함.
 * 목표: 서로 다른 트랜잭션(Update vs Insert+Update) 간의 충돌 유도
 */

// ==================== 메트릭 설정 ====================
const deadlockCounter = new Counter('deadlock_errors'); // 데드락 발생 수
const errorRate = new Rate('error_rate'); // 전체 에러율

// ==================== 테스트 옵션 ====================
export const options = {
    scenarios: {
        // "Chaos Mode": 100명이 동시에 덤벼듦
        chaos_mix: {
            executor: 'constant-vus',
            vus: 100,              // 동시 접속자 100명
            duration: '1m',        // 1분 동안 지속
            exec: 'mixRequest',    // 아래 mixRequest 함수 실행
        },
    },
    thresholds: {
        // 데드락은 0건이어야 통과
        deadlock_errors: ['count==0'],
    },
};

// ==================== 설정 ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'; // 포트 확인!
const API_BASE = `${BASE_URL}/api/v1/community`;
const TARGET_POST_ID = 1; // 1번 게시글 집중 공격

const getHeaders = () => {
    return {
        'Content-Type': 'application/json',
        // 'Authorization': 'Bearer ...' // 필요시 토큰 추가
    };
};

// ==================== 핵심 로직 ====================
export function mixRequest() {
    // 0 ~ 1 사이 난수 생성
    const rand = Math.random();
    let res;
    let actionType = "";

    // 🎲 33% 확률로 조회수 증가 (Update View)
    if (rand < 0.33) {
        actionType = "VIEW";
        res = http.post(
            `${API_BASE}/posts/${TARGET_POST_ID}/views`,
            null,
            { headers: getHeaders(), tags: { name: 'Mix_View' } }
        );
    }
    // 🎲 33% 확률로 좋아요 토글 (Update Like + Insert/Delete Like Table)
    else if (rand < 0.66) {
        actionType = "LIKE";
        res = http.post(
            `${API_BASE}/posts/${TARGET_POST_ID}/likes`,
            null,
            { headers: getHeaders(), tags: { name: 'Mix_Like' } }
        );
    }
    // 🎲 33% 확률로 댓글 작성 (Update CommentCount + Insert Comment Table)
    else {
        actionType = "COMMENT";
        const payload = JSON.stringify({ content: "데드락 테스트 댓글입니다." });
        res = http.post(
            `${API_BASE}/posts/${TARGET_POST_ID}/comments`,
            payload,
            { headers: getHeaders(), tags: { name: 'Mix_Comment' } }
        );
    }

    // ==================== 결과 검증 ====================
    const success = check(res, {
        '2xx OK': (r) => r.status >= 200 && r.status < 300,
    });

    if (!success) {
        errorRate.add(1);
        const body = res.body ? String(res.body) : "";

        // 데드락 에러 감지 (Deadlock found ... 메시지 체크)
        if (res.status === 500 && body.includes('Deadlock')) {
            deadlockCounter.add(1);
            console.error(`💀 Deadlock detected! Action: ${actionType}`);
        }
    }

    // ⚠️ sleep 없음: 쉴 새 없이 공격해서 충돌 확률 극대화
}

// ==================== 리포트 ====================
export function handleSummary(data) {
    const deadlocks = data.metrics.deadlock_errors ? data.metrics.deadlock_errors.values.count : 0;

    return {
        'stdout': `
    =========================================
    🧨 CHAOS MIX TEST RESULT
    =========================================
    💀 Deadlocks Detected : ${deadlocks}
    =========================================
    ${deadlocks > 0 ? "⛔ FAILED: 데드락 발생함" : "✅ PASSED: 데드락 방어 성공"}
    =========================================
    `
    };
}