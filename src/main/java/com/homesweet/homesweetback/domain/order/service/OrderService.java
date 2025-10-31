package com.homesweet.homesweetback.domain.order.service;

// --- DTO Imports ---

import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.common.exception.PaymentMismatchException;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.response.MyOrderItemResponse;
import com.homesweet.homesweetback.domain.order.dto.response.OrderDetailResponse;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;

// --- Entity Imports ---
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;

// --- Repository Imports ---
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;

// --- Exception Imports ---
import jakarta.persistence.EntityNotFoundException;

// --- Spring & Java Imports ---
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    // --- (수정) 주문 생성에 필요한 Repository만 주입 ---
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SkuJPARepository skuJPARepository;
    private final PaymentRepository paymentRepository;


    /**
     * API 1: 주문 생성 (결제 준비)
     * (재고 검증 로직은 PaymentService로 이동)
     */
    @Transactional
    public OrderReadyResponse createOrder(CreateOrderRequest dto, Long userId) {

        // 1. 사용자(User) 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        List<OrderItem> orderItemsList = new ArrayList<>();
        long totalAmount = 0L;
        int totalShippingPrice = 0;

        // 2. 주문 항목(SKU) 조회 및 총액 계산
        for (CreateOrderRequest.OrderItemRequest itemDto : dto.orderItems()) {
            // 2-1. SKU 조회 (ProductEntity 포함)
            // (락 불필요, 재고 검증 안 함)
            SkuEntity sku = skuJPARepository.findById(itemDto.skuId())
                    .orElseThrow(() -> new EntityNotFoundException("SKU를 찾을 수 없습니다: " + itemDto.skuId()));

            ProductEntity product = sku.getProduct();

            // 2-2. 주문 항목 가격 계산 (할인 적용된 '단가')
            long discountedPrice = calculateDiscountedPrice(product.getBasePrice(), sku.getPriceAdjustment(), product.getDiscountRate());

            // 2-3. 총 주문 금액 계산 (상품 총액)
            totalAmount += (discountedPrice * itemDto.quantity());

            // 2-4. 총 배송비 계산
            totalShippingPrice += product.getShippingPrice();

            // 2-5. OrderItem 엔티티 생성
            OrderItem orderItem = OrderItem.builder()
                    .sku(sku)
                    .quantity((long) itemDto.quantity())
                    .price(discountedPrice) // 주문 시점의 '단가' 스냅샷
                    .build();
            orderItemsList.add(orderItem);
        }

        // 3. 최종 결제 금액 = 상품 총액 + 총 배송비
        totalAmount += totalShippingPrice;

        // 4. Order 엔티티 생성 (PENDING, BEFORE_SHIPMENT 상태)
        Order order = Order.builder()
                .user(user)
                .orderStatus(OrderStatus.PENDING)
                .deliveryStatus(DeliveryStatus.BEFORE_SHIPMENT)
                .totalAmount(totalAmount)
                .build();

        // 5. 연관관계 설정 (Order <-> OrderItem)
        for (OrderItem item : orderItemsList) {
            order.addOrderItem(item);
        }

        // 6. DB에 저장 (Order, OrderItem 동시 저장)
        Order savedOrder = orderRepository.save(order);

        // 7. OrderReadyResponse DTO 생성
        String orderNumber = savedOrder.generateOrderNumber();

        List<OrderReadyResponse.OrderItemDetail> itemDetails = savedOrder.getOrderItems().stream()
                .map(oi -> {
                    SkuEntity sku = oi.getSku();
                    ProductEntity product = sku.getProduct();
                    String optionName = "옵션명 (수정 필요)";

                    long finalItemPrice = oi.getPrice() * oi.getQuantity();

                    return new OrderReadyResponse.OrderItemDetail(
                            oi.getId(),
                            product.getImageUrl(),
                            product.getBrand(),
                            product.getName(),
                            optionName,
                            product.getBasePrice(),
                            product.getDiscountRate(),
                            product.getShippingPrice(),
                            finalItemPrice, // (할인된 단가 * 수량)
                            oi.getQuantity().intValue()
                    );
                }).collect(Collectors.toList());

        return new OrderReadyResponse(
                savedOrder.getId(),
                orderNumber,
                user.getName(),
                user.getAddress(),
                user.getPhoneNumber(),
                itemDetails,
                savedOrder.getTotalAmount(),
                totalShippingPrice
        );
    }

    public List<MyOrderItemResponse> getMyOrders(Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        // 2. 주문 목록 조회 (N+1 방지용 쿼리 사용)
        List<Order> orders = orderRepository.findAllByUserWithDetails(user);
        // 3. MyOrderItemResponse DTO 리스트로 변환
        return orders.stream().map(order -> {
                    // 3-1. 대표 상품 찾기
                    OrderItem representativeItem = order.getOrderItems().stream().findFirst().orElse(null);

                    String productName = "주문 항목 없음";
                    String imageUrl = "";
                    Long price = 0L; // 대표 상품 단가

                    if (representativeItem != null) {
                        ProductEntity product = representativeItem.getSku().getProduct();
                        productName = product.getName();
                        imageUrl = product.getImageUrl();
                        price = representativeItem.getPrice();
                    }

                    // 3-2. DTO 생성
                    return MyOrderItemResponse.builder()
                            .orderId(order.getId())
                            .orderNumber(order.generateOrderNumber())
                            .orderDate(order.getOrderedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .productName(productName)
                            .imageUrl(imageUrl)
                            .price(price)
                            .orderStatus(order.getOrderStatus().name())
                            .deliveryStatus(order.getDeliveryStatus().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // (상품 가격 계산 헬퍼 메서드)
    private long calculateDiscountedPrice(Integer basePrice, Integer adjustment, BigDecimal discountRate) {
        long pricePerItem = basePrice + adjustment;
        return pricePerItem - (long) (pricePerItem * discountRate.doubleValue());
    }

    // 주문 상세 조회
    public OrderDetailResponse getOrderDetail(Long orderId, Long userId) {

        // 1. 주문 조회 (N+1 방지를 위해 모든 연관 엔티티를 fetch join)
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderId));

        // 2. (보안) 주문자 본인 확인
        if (!order.getUser().getId().equals(userId)) {
            // (권한 없음 - 403 Forbidden이 더 적절할 수 있음)
            throw new PaymentMismatchException("주문 정보에 접근할 권한이 없습니다.");
        }

        // 3. 결제 정보 조회
        // (결제가 PENDING 상태일 경우 Payment가 없을 수 있으므로 orElse(null) 처리)
        Payment payment = paymentRepository.findByOrder(order)
                .orElse(null); // 결제 대기중인 주문은 payment가 null일 수 있음

        // 4. 총 배송비 계산 (단순 합산)
        // (OrderReadyResponse DTO 정의에 따라 Integer로 반환)
        Integer totalShippingPrice = order.getOrderItems().stream()
                .mapToInt(item -> item.getSku().getProduct().getShippingPrice())
                .sum();

        // 5. DTO로 변환
        return OrderDetailResponse.of(order, payment, order.getUser(), totalShippingPrice);
    }
}