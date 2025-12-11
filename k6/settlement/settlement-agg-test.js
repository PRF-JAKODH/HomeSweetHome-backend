import http from "k6/http";
import { check, sleep } from "k6";

const TOKEN = __ENV.TOKEN;
const BASE_URL = __ENV.BASE_URL;

export const options = {
    scenarios: {
        batch_runner: {
            executor: "constant-arrival-rate",
            rate: 1,              // 10초마다 1번 실행
            timeUnit: "10s",
            duration: "10m",      // 총 60번 실행 (10초 × 60)
            preAllocatedVUs: 1,
            maxVUs: 1,
            exec: "runBatch",
        }
    },
    thresholds: {
        http_req_duration: ["p(95)<8000"], // 배치 완료 8초 이하인지 체크
        http_req_failed: ["rate<0.05"],
    },
};

export function runBatch() {
    const res = http.post(`${BASE_URL}/api/v1/settlement/batch/run`, null, {
        headers: {
            Authorization: `Bearer ${TOKEN}`,
        },
    });

    check(res, {
        "Batch run OK": (r) => r.status === 200,
    });

    sleep(10);
}
