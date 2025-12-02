package com.homesweet.homesweetback.domain.order.adapter;

import com.homesweet.homesweetback.common.exception.TossApiFailedException;
import com.homesweet.homesweetback.common.util.PaymentApiClient;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * [부하 테스트 전용] 실제 Toss API 호출을 건너뛰고 성공 응답을 반환하는 Mock Adapter
 */
@Slf4j
@Component // 프로필일 때만 이 Bean이 활성화됩니다.
public class TossPaymentsMockAdapter extends TossPaymentsAdapter {

    // 👇 [신규] 생성자를 명시적으로 정의하고 super()를 호출합니다.
    public TossPaymentsMockAdapter(PaymentApiClient paymentApiClient,
                                   @Value("${payments.toss.secretKey}") String tossSecretKey) { // 👈 이 코드를 확인
        super(paymentApiClient);
    }

    // [API 1] 결제 승인 - Mock 구현
    @Override
    @CircuitBreaker(name = "toss-payments", fallbackMethod = "fallbackConfirmPayment")
    public Map<String, Object> confirmPaymentToToss(PaymentConfirmRequest dto) {

        // 1. [유지] 지연 시뮬레이션 (2초)
//        try{
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt(); // 인터럽트 상태 복구 (관례)
//            throw new RuntimeException("부하 테스트 지연 중 스레드 인터럽트 발생", e);
//        }

        // 2. [핵심] 실제 API 호출 없이, 성공 응답 Map을 반환
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("paymentKey", "MOCK_PK_" + dto.paymentKey());
        mockResponse.put("method", "카드");
        mockResponse.put("status", "DONE"); // 성공 시그널
        mockResponse.put("paidAt", "2025-11-19T17:00:00");

        log.info("[MOCK SUCCESS] Toss API call skipped. Returning success response.");
        return mockResponse;
    }

    // [API 2] 취소 - Mock 구현
    @Override
    public Map<String, Object> cancelPaymentToToss(String paymentKey, String cancelReason) {
        log.info("[MOCK SUCCESS] Toss API cancel skipped. Returning CANCELED status.");
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("status", "CANCELED");
        return mockResponse;
    }

    // (fallbackConfirmPayment 메서드는 TossPaymentsAdapter에서 상속받아 사용합니다.)
}