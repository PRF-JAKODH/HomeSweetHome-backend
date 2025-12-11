import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL;
const TOKEN = __ENV.SELLER_TOKEN;

// ================================
// 시나리오 설정 (RPS 최적화 버전)
// ================================
export const options = {
    scenarios: {
        // ---------------------------
        // 1) Daily Settlement Peak Test
        // ---------------------------
        daily_test: {
            executor: "constant-arrival-rate",
            rate: 5,                   // 5 RPS
            timeUnit: "1s",
            duration: "5m",
            preAllocatedVUs: 10,
            maxVUs: 20,
            exec: "runDaily",
        },

        // ---------------------------
        // 2) Weekly Settlement Peak Test
        // ---------------------------
        weekly_test: {
            executor: "constant-arrival-rate",
            rate: 2,                   // 2 RPS
            timeUnit: "1s",
            duration: "5m",
            preAllocatedVUs: 8,
            maxVUs: 20,
            exec: "runWeekly",
        },

        // ---------------------------
        // 3) Monthly Settlement Peak Test
        // ---------------------------
        monthly_test: {
            executor: "constant-arrival-rate",
            rate: 1,                 // 0.5 RPS (2초에 1회)
            timeUnit: "1s",
            duration: "5m",
            preAllocatedVUs: 5,
            maxVUs: 10,
            exec: "runMonthly",
        },

        // ---------------------------
        // 4) Yearly Settlement Peak Test
        // ---------------------------
        yearly_test: {
            executor: "constant-arrival-rate",
            rate: 1,                 // 0.2 RPS (5초에 1회)
            timeUnit: "1s",
            duration: "5m",
            preAllocatedVUs: 3,
            maxVUs: 10,
            exec: "runYearly",
        },
    },

    thresholds: {
        http_req_duration: ["p(95)<3000"],     // SLA: 3초 내 응답
        http_req_failed: ["rate<0.05"],
    },
};

// ================================
// Logging helper
// ================================
function logResponse(label, res) {
    console.log(`\n[${label}]`);
    console.log(`URL: ${res.url}`);
    console.log(`STATUS: ${res.status}`);
    console.log(`BODY: ${res.body}\n`);
}

// ================================
// Test Functions
// ================================

// 1) Daily Settlement
export function runDaily() {
    const url = `${BASE_URL}/api/v1/settlement/daily/10000/generate?startDate=2025-12-08&endDate=2025-12-08`;

    const res = http.post(url, null, {
        headers: { Authorization: `Bearer ${TOKEN}` },
    });

    logResponse("DAILY", res);
    check(res, { "Daily OK": (r) => r.status === 200 });
}

// 2) Weekly Settlement
export function runWeekly() {
    const url = `${BASE_URL}/api/v1/settlement/weekly/10000/generate?weekStart=2025-12-08&weekEnd=2025-12-14`;

    const res = http.post(url, null, {
        headers: { Authorization: `Bearer ${TOKEN}` },
    });

    logResponse("WEEKLY", res);
    check(res, { "Weekly OK": (r) => r.status === 200 });
}

// 3) Monthly Settlement
export function runMonthly() {
    const url = `${BASE_URL}/api/v1/settlement/monthly/10000/generate`;

    const res = http.post(url, null, {
        headers: { Authorization: `Bearer ${TOKEN}` },
    });

    logResponse("MONTHLY", res);
    check(res, { "Monthly OK": (r) => r.status === 200 });
}

// 4) Yearly Settlement
export function runYearly() {
    const url = `${BASE_URL}/api/v1/settlement/yearly/10000/generate`;

    const res = http.post(url, null, {
        headers: { Authorization: `Bearer ${TOKEN}` },
    });

    logResponse("YEARLY", res);
    check(res, { "Yearly OK": (r) => r.status === 200 });
}
