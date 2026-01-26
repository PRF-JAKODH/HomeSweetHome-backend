package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.domain.order.dto.PaymentResponse;
import com.homesweet.homesweetback.domain.order.dto.TossPaymentCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.TossPaymentConfirmRequest;

/**
 * 결제 서비스 인터페이스
 * TossPaymentsService를 활용한 결제 비즈니스 로직 처리
 */
public interface PaymentService {

    /**
     * 결제 승인 처리
     * 토스페이먼츠 결제창 완료 후 successUrl에서 호출
     * 
     * 토스페이먼츠 문서에 따라:
     * 1. 쿼리 파라미터의 amount와 요청 시 보낸 amount가 같은지 확인 (클라이언트 금액 조작 방지)
     * 2. paymentKey, amount, orderId 저장
     * 3. 결제 승인 API 호출
     *
     * @param userId 사용자 ID
     * @param request 결제 승인 요청 (paymentKey, orderId, amount)
     * @return 결제 응답
     */
    PaymentResponse confirmPayment(Long userId, TossPaymentConfirmRequest request);

    /**
     * 결제 취소
     * 이미 결제된 주문의 결제 취소 처리
     *
     * @param userId 사용자 ID
     * @param paymentKey 결제 키
     * @param request 취소 요청 (취소 사유, 부분 취소 금액)
     * @return 결제 응답
     */
    PaymentResponse cancelPayment(Long userId, String paymentKey, TossPaymentCancelRequest request);

    /**
     * 결제 조회
     *
     * @param userId 사용자 ID
     * @param paymentKey 결제 키
     * @return 결제 응답
     */
    PaymentResponse getPayment(Long userId, String paymentKey);
}
