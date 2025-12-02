import http from 'k6/http';
import { check, sleep } from 'k6';
// UUID를 생성하여 주문번호 중복 에러를 방지하는 유틸리티
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {

    stages: [
        { duration: '1m', target: 3800 },
        { duration: '3m', target: 3800 },
        { duration: '1m', target: 0 },
    ],

    // vus: 500,
    // duration: '60s', // N초 동안 테스트
    // 임계값(Thresholds): 95%의 응답 시간이 5초를 넘기면 테스트를 실패로 간주
    thresholds: {
        http_req_duration: ['p(95) < 5000'],
        'checks': ['rate==1'], // 성공률 100%
    },
};

export default function () {
    const userId = 1; // 테스트할 사용자 ID (토큰의 사용자 ID와 일치해야 함)
    const productSkuId = 3;  // 테스트할 SKU ID (DB에 존재해야 함)

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer 1' };

    // --- 1단계: 주문 생성 (CREATE) ---
    const orderCreationPayload = JSON.stringify({
        orderItems: [{
            skuId: productSkuId,
            quantity: 1
        }],

        recipientName: "Test Name",
        recipientPhone: "010-1234-5678",
        shippingAddress: "Test Address",
        shippingRequest: "",
    });

    const createRes = http.post('http://localhost:8080/api/v1/orders', orderCreationPayload, { headers: headers });

    // 💡 검증: 주문 생성이 200/201로 성공했는지 확인
    check(createRes, {
        '01. CREATE Status 200/201': (r) => r.status === 200 || r.status === 201,
    });

    // 주문 생성이 실패했으면, 다음 단계(Confirm)로 진행하지 않습니다.
    if (createRes.status !== 200 && createRes.status !== 201) {
        return;
    }

    // 응답에서 주문번호 추출 (OrderReadyResponse의 orderNumber 필드를 가정)
    const orderData = createRes.json();
    const orderNumber = orderData.orderNumber;
    const totalAmount = orderData.totalAmount;


    // --- 2단계: 결제 승인 (CONFIRM) ---
    const confirmPayload = JSON.stringify({
        paymentKey: `pk_k6_test_${orderNumber}`,
        orderId: orderNumber, // 1단계에서 생성된 주문번호 사용
        amount: totalAmount,  // 1단계에서 계산된 최종 금액 사용
    });

    const confirmRes = http.post('http://localhost:8080/api/v1/orders/payments/confirm', confirmPayload, { headers: headers });

    //  검증: 결제 승인이 200/201로 성공했는지 확인
    check(confirmRes, {
        '02. CONFIRM Status 200/201': (r) => r.status === 200 || r.status === 201,
    });

    sleep(0.1);
}