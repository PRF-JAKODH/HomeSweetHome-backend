import http from "k6/http";
import { check, sleep } from "k6";

// String key = "sku:" + skuId + ":stock";

// JWT token
const SELLER_TOKEN = __ENV.SELLER_TOKEN;
const BASE_URL = __ENV.BASE_URL;
const USER_TOKEN = __ENV.USER_TOKEN; // 테스트할 사용자 ID (토큰의 사용자 ID와 일치해야 함)


export const options = {
    discardResponseBodies: false,
    scenarios: {
        // 1) 주문 유입 시뮬레이션 (steady load)
        order_inflow: {
            executor: "constant-arrival-rate",
            rate: 500,              // 초당 0.1건 → 하루 약 1만건 -> 5분동안 9000건
            timeUnit: "1s",
            duration: "10m",         // 10분 동안 테스트
            preAllocatedVUs: 50,
            maxVUs: 1000,
            exec: "orderFlow",
        },

        batch_runner: {
            executor: "per-vu-iterations",
            vus: 1,
            iterations: 60,     // 10초 × 60 = 10분
            maxDuration: "20m",
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
    const productSkuId = 300001;  // 테스트할 SKU ID (DB에 존재해야 함)
    const url = `${BASE_URL}/api/v1/orders`;
    const payload = JSON.stringify({
        orderItems: [{
            skuId: productSkuId,
            quantity: 1
        }],
        recipientName: "Test Name",
        recipientPhone: "010-1234-5678",
        shippingAddress: "Test Address",
        shippingRequest: "",
    });

    const params = {
        headers: {
            Authorization: `Bearer ${USER_TOKEN}`,
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

    const url = `${BASE_URL}/api/v1/orders/payments/confirm`;
    const params = {
        headers: {
            Authorization: `Bearer ${USER_TOKEN}`,
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
    const url = `${BASE_URL}/api/v1/settlement/batch/run`;

    const params = {
        headers: {
            Authorization: `Bearer ${SELLER_TOKEN}`,
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
    sleep(0.4);
    confirmPayment(order);
    sleep(1);
}