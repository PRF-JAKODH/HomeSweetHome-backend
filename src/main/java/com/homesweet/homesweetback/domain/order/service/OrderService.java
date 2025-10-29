package com.homesweet.homesweetback.domain.order.service;

// --- DTO Imports ---
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;

// --- Entity Imports ---
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
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
            // (TODO: 배송비 정책 확인 필요 - 여기서는 단순 합산)
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
                    // TODO: 옵션 이름 조합 로직 필요 (sku.getSkuOptions())
                    String optionName = "옵션명 (수정 필요)";

                    // TODO: finalPrice 계산 로직 검토 필요
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

    // (상품 가격 계산 헬퍼 메서드)
    private long calculateDiscountedPrice(Integer basePrice, Integer adjustment, BigDecimal discountRate) {
        long pricePerItem = basePrice + adjustment;
        return pricePerItem - (long) (pricePerItem * discountRate.doubleValue());
    }
}