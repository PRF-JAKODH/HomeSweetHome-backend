package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.common.exception.PaymentMismatchException;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.request.PaymentConfirmRequest;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;
import com.homesweet.homesweetback.domain.order.dto.response.PaymentConfirmResponse;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.Payment;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
//상품


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

// --상품--
//나중에 상품 레포지토리 import

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    //나중에 상품 도메인

    @Value("${payments.toss.secretKey}")
    private String tossSecretKey;



    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    //=== API 1 : 주문 생성 ===
    @Transactional //(중요) 데이터를 DB에 쓰기 때문에 readOnly=false 트랜잭션 필요
    public OrderReadyResponse createOrder(CreateOrderRequest dto, Long userId) {
        //(임시) 총액 계산 및 주문 이름 생성(Mock)
        Long totalAmount = calculateMockTotalPrice(dto.orderItems());
        String orderName = createOrderName(dto.orderItems());

        //2. 상점 주문번호(merchant_uid) 생성 (UUID 사용)
        String merchantUid = UUID.randomUUID().toString();

        // 3. Order 엔티티 생성 (Builder 사용)
        Order order = Order.builder()
                .merchantUid(merchantUid)
                .status(OrderStatus.PENDING) // ★ 최초 상태: PENDING
                .totalAmount(totalAmount)
                .userId(userId) // (추후 User 엔티티 연동)
                .recipientName(dto.recipientName()) // (DTO에 추가 필요. DTO 수정!)
                .recipientPhone(dto.recipientPhone()) // (DTO에 추가 필요. DTO 수정!)
                .shippingAddress(dto.shippingAddress())
                .shippingRequest(dto.shippingRequest()) // (DTO에 추가 필요. DTO 수정!)
                .build();

        // 4. OrderItem 엔티티 생성 및 Order에 추가
        for (CreateOrderRequest.OrderItemRequest itemDto : dto.orderItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemDto.productId())
                    .quantity(itemDto.quantity())
                    .unitPrice(10000L) // (임시) TODO: 실제 상품 가격
                    .build();

            // ★ (중요) 양방향 연관관계 편의 메서드 사용
            order.addOrderItem(orderItem);
        }
        // 5. DB에 저장(CascadeType.ALL 덕분에 OrderItem도 함께 저장됨)
        orderRepository.save(order);
        // 6. 프론트에 반환할 DTO 생성
        return new OrderReadyResponse(merchantUid, orderName, totalAmount);
    }
    //=== API 2: 결제 검증 및 완료 ===
    @Transactional // DB에 Payment 저장, Order 상태 변경
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest dto, Long userId) {
        // 1. 검증 1 - merchant_uid (dto.orderId)로 DB에서 Order조회
        Order order = orderRepository.findByMerchantUid(dto.orderId())
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + dto.orderId()));

        // 2. 검증 2 - (보안) 요청한 유저(userId)가 주문한 유저가 맞는지 확인
        // 추후

        // 3. 검증 3 - (핵심) DB의 주문 금액과 토스가 전달한 결제 금액이 일치하는지 확인
        if (!order.getTotalAmount().equals(dto.amount())) {
            //금액 위변조 의심. 결제 취소 API를 호출해야 함.
            throw new PaymentMismatchException("결제 금액이 일치하지 않습니다.");
        }

        // 4. 검증 4 - (멱등성)
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new PaymentMismatchException("이미 처리된 주문입니다.");
        }

        // 5. [핵 to the 심] 토스페이먼츠 결제 승인 API 호출
        // (RestTemplate/ObjectMapper는 Config로 Bean 등록 후 주입받는 것이 Best Practice)
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        // 5-1. Basic Auth 헤더 준비 (시크릿 키 Base64 인코딩)
        HttpHeaders headers = new HttpHeaders();
        String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.setBasicAuth(encodedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 5-2. 요청 바디(Body) 준비 (DTO와 동일)
        HttpEntity<PaymentConfirmRequest> requestEntity = new HttpEntity<>(dto, headers);

        // 5-3. API 호출
        Map<String, Object> tossResponse;
        try {
            //postForEntity(API URL, 요청객체, 응답받을 타입)
            tossResponse = restTemplate.postForObject(TOSS_CONFIRM_URL, requestEntity, Map.class);
        } catch (Exception e) {
            // (TODO: 토스 API 통신 실패. 결제 취소 API를 호출해야 할 수도 있음)
            throw new RuntimeException("토스페이먼츠 승인 API 호출에 실패했습니다." + e.getMessage());
        }

        // 6. [DB 저장 1] 토스 응답 기반으로 Payment 엔티티 생성
        String tossPaymentKey = (String) tossResponse.get("paymentKey");
        String method = (String) tossResponse.get("method");
        String status = (String) tossResponse.get("status");

        if (!"DONE".equals(status)) {
            // 결제가 완료되지 않음. 실패 처리
            throw new RuntimeException("결제가 완료되지 않았습니다. 상태: " + status);
        }

        // paidAt 문자열을 가져옴
        String paidAtString = (String) tossResponse.get("paidAt");
        // LocalDateTime으로 파싱함.
        LocalDateTime paidAt = (paidAtString != null) ? LocalDateTime.parse(paidAtString) : null;

        Payment payment;
        try {
            payment = Payment.builder()
                    .order(order)
                    .pgTransactionId(tossPaymentKey)
                    .amount(order.getTotalAmount())
                    .method(method)
                    .paymentStatus(status)
                    .paidAt(paidAt)
                    .pgRawData(objectMapper.writeValueAsString(tossResponse)) // (중요) 응답 원본 저장
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Payment 엔티티 생성 중 오류 발생");
        }

        paymentRepository.save(payment);

        // 7. [DB 저장 2] Order 상태 변경 (PENDING -> DELIVERED)
        // 우리는 결제 완료 시 즉시 '배송 완료'로 처리
        order.changeStatus(OrderStatus.DELIVERED);
        // @Transactional이 끝날 때, 변경된 'order' 엔티티는 자동 UPDATE (더티 체킹)

        // 8.최종 응답 반환
        return new PaymentConfirmResponse(order.getMerchantUid(), order.getStatus().name());
    }
    // ================== (임시) Mock 헬퍼 메서드 ==================
    // ProductRepository

    private Long calculateMockTotalPrice(List<CreateOrderRequest.OrderItemRequest> items) {
        long total = 0L;
        for (CreateOrderRequest.OrderItemRequest item : items) {
            total += (long) item.quantity() * 10000L; // (임시) 모든 상품 가격 10000원으로 고정
        }
        return total;
    }

    private String createOrderName(List<CreateOrderRequest.OrderItemRequest> items) {
        if (items.isEmpty()) {
            return "주문 없음";
        }
        String firstItemName = "상품 " + items.get(0).productId(); // (임시) "상품 1"
        if (items.size() > 1) {
            return firstItemName + " 외 " + (items.size() - 1) + "건";
        }
        return firstItemName;
    }
}