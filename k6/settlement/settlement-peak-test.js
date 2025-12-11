import http from "k6/http";      // 중요: k6/http 말고 mock.js 사용
import { check, sleep } from "k6";

// JWT token
const TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsImVtYWlsIjoiaHNrd29vbjdAZ21haWwuY29tIiwibmFtZSI6Iu2drOyImCIsInByb3ZpZGVyIjoiZ29vZ2xlIiwicm9sZSI6IlNFTExFUiIsImlhdCI6MTc2NDgzNDk2MCwiZXhwIjoxNzY0ODUyOTYwfQ.BVdW2zP2r_qzihEp21wlPSiqsVjOlpJGmZzA4NB-gmwktTbP3NMwSoYjcesyhuM_ild2BG5BG8jV5LXhh-QEpw";

export const options = {
    discardResponseBodies: false,
        peak_load: {
            executor: "ramping-arrival-rate",
            startRate: 100,     // 워밍업
            timeUnit: "1s",
            preAllocatedVUs: 5000,
            maxVUs: 15000,

            stages: [
                { target: 500, duration: "30s" },   // 안정 구간
                { target: 1000, duration: "20s" },  // 꾸준히 증가
                { target: 2000, duration: "10s" },  // 🔥 피크 순간
                { target: 0, duration: "10s" },     // 급락 → 회복 여부 확인
            ],

            exec: "orderFlow",
        },

        batch_runner: {
            executor: "per-vu-iterations",
            vus: 1,
            iterations: 30,
            maxDuration: "10m",
            exec: "runBatch",
        }
    },

    thresholds: {
        // 주문 생성 API SLA
        http_req_failed: ["rate<0.05"],
        http_req_duration: ["p(95)<1500"],

        // 배치 실행은 따로 커스텀 측정
    },
};

function safeJson(res) {
    if (!res || !res.body) return null;
    try {
        return JSON.parse(res.body);
    } catch (e) {
        console.error("JSON parse error:", e);
        return null;
    }
}

// 주문 생성 (steady inflow)
function createOrder() {
    const url = "http://localhost:8080/api/v1/orders";
    const payload = JSON.stringify({
        orderItems: [{
            skuId: 2,
            quantity: 1,
            price: 480000,
        }]
    });

    const params = {
        headers: {
            Authorization: `Bearer ${TOKEN}`,
            "Content-Type": "application/json",
        },
    };

    let res = http.post(url, payload, params);

    check(res, {
        "Order created": (r) => r.status === 200,
    });
    if (res.status !== 200 || !res.body) {
        console.error("Order API failed:", res.status);
        console.log(`Order response body: ${res.body}`);
        return null;
    }

    const body = safeJson(res);

    if (!body) {
        return {
            orderId: null,
            orderNumber: null,
        };
    }

    return {
        orderId: body.orderId,
        orderNumber: body.orderNumber,
        totalAmount: body.totalAmount
    };
    // sleep(1);
}

// 결제 완료
function confirmPayment(order) {
    const payload = JSON.stringify({
        orderId: order.orderNumber,
        paymentKey: `paykey-${order.orderId}-${Math.random()}`,
        amount: order.totalAmount,
        method: "CARD"
    });

    const url = "http://localhost:8080/api/v1/orders/payments/confirm";
    const params = {
        headers: {
            Authorization: `Bearer ${TOKEN}`,
            "Content-Type": "application/json",
        },
    };

    let res = http.post(url, payload, params);

    check(res, {
        "Payment success": (r) => r.status === 200,
    });
    // sleep(1);
}

// 배치 실행
export function runBatch() {
    const url = "http://localhost:8080/api/v1/settlement/batch/run";

    const params = {
        headers: {
            Authorization: `Bearer ${TOKEN}`,
        },
    };

    const res = http.post(url, null, params);

    check(res, {
        "Batch started": (r) => r.status === 200,
    });
    sleep(10);
}

export function orderFlow() {
    const order = createOrder();

    if (!order) {
        console.log("order= {}", order.orderId)
        console.error("Order creation failed — skipping payment");
        return;
    }
    // sleep(0.4);
    confirmPayment(order);
    // sleep(1);
}