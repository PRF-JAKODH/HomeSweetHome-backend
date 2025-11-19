package com.homesweet.homesweetback.domain.order.service;

// --- DTO Imports ---
import com.homesweet.homesweetback.domain.order.adapter.TossPaymentsAdapter;
import com.homesweet.homesweetback.domain.order.dto.request.OrderCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;

// --- Entity Imports ---
import com.homesweet.homesweetback.domain.order.entity.*;

// --- Repository Imports ---
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;

// --- Exception Imports ---
import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.common.exception.PaymentMismatchException;
import jakarta.persistence.EntityNotFoundException;

// --- Spring & Java Imports ---
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    // --- (수정) 결제 처리에 필요한 Repository 및 Bean 주입 ---
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentsAdapter tossPaymentsAdapter;
    private final PaymentProcessor paymentProcessor;

    /**
     * API 2: 결제 검증 및 완료
     * (재고 차감 로직 포함)
     */
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest dto, Long userId) {

        // 1. Order ID (PK)로 DB에서 Order 조회
        Order order = orderRepository.findByOrderNumber(dto.orderId())
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + dto.orderId()));
        log.debug(order.toString());
        log.debug(order.getOrderStatus().toString());
        log.debug(order.getTotalAmount().toString());
        log.debug(dto.amount().toString());


        //TODO: 현재 아키텍쳐 잘 짜셧는데, 도메인에 핏한 기능들이 결제쪽에서 처리하는게 맞을까? v
        order.validateOwner(userId);
        order.validatePaymentAmount(dto.amount());
        order.validatePaymentStatus();

        //TODO: 결제가 됬는데 배송이 안와요 (언포)기븐이 안좋겟죠(개발자가 잘 처리해야합니다) v
        //TODO: 결국 케이스 마다 쪼개시다보면 그게 TC, 트랜잭션을 자연스럽게 분리하게 됩니다. v
        //5. 외부 API 호출 - 토스 페이먼츳
        Map<String, Object> tossResponse;
        try{
            tossResponse = tossPaymentsAdapter.confirmPaymentToToss(dto);
        } catch (Exception e) {
            //TODO: 환불 재고 롤백은 있으나, 예외에 대한 재고 롤백이 없네요 v
            paymentProcessor.processPaymentFailDB(order);
            throw e;
        }

        paymentProcessor.processPaymentSuccessDB(order, tossResponse, userId);

        return new PaymentConfirmResponse(
                order.getId(),
                order.getOrderStatus().name()
        );
    }

    /**
     * API 3: 주문 취소 (환불)
     * (토스 API 호출, 재고 복구)
     */
    public void cancelOrder(Long orderId, Long userId, OrderCancelRequest dto) {

        // 1. 주문 조회 (모든 연관 엔티티 포함)
        Order order = orderRepository.getByIdWithDetailsOrThrow(orderId);

        // 2. 주문자 확인
        order.validateOwner(userId);

        // 3. 이미 취소된 주문인지 확인
        order.validateCancelStatus();

        // 4. 결제 정보 조회
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다. (비정상 상태)"));

        // 5. (★핵심★) 토스페이먼츠 결제 취소 API 호출
        // 5. (외부 API 호출) [수정] 트랜잭션 "밖에서" 어댑터 호출
        Map<String, Object> tossResponse;
        try {
            String tossPaymentKey = payment.getPgTransactionId();
            tossResponse = tossPaymentsAdapter.cancelPaymentToToss(tossPaymentKey, dto.cancelReason());

        } catch (Exception e) {
            log.error("토스페이먼츠 취소 API 호출 실패: {}", e.getMessage());
            // (정책 필요) API 호출 실패 시 DB 롤백을 할 필요가 없으므로,
            // DB 상태를 변경하지 않고 예외만 던집니다.
            throw new RuntimeException("결제 취소 API 호출에 실패했습니다. " + e.getMessage());
        }

        // 6. (내부 DB 처리) [신규] API 취소가 성공하면, DB 작업용 트랜잭션 메서드 호출
        paymentProcessor.processPaymentCancelDB(order, payment, tossResponse);
    }

}