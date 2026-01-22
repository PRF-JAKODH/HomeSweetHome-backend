package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.dto.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.OrderResponse;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.CartJPARepository;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 주문 서비스 구현체
 * 장바구니 -> 주문 생성 및 주문 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartJPARepository cartJPARepository;
    private final UserRepository userRepository;

    /**
     * 장바구니에서 주문 생성
     * 토스페이먼츠 결제창 호출 전에 주문을 먼저 생성
     */
    @Override
    @Transactional
    public OrderResponse createFromCart(Long userId, CreateOrderRequest request) {
        log.info("주문 생성 시작: userId={}, cartIds={}", userId, request.getCartIds());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 장바구니 아이템 조회
        List<CartEntity> cartItems = cartJPARepository.findAllById(request.getCartIds());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("장바구니에서 선택한 상품을 찾을 수 없습니다.");
        }

        // 본인의 장바구니가 맞는지 확인
        boolean hasInvalidCart = cartItems.stream()
                .anyMatch(cart -> !cart.getUser().getId().equals(userId));
        if (hasInvalidCart) {
            throw new IllegalArgumentException("본인의 장바구니 상품만 주문할 수 있습니다.");
        }

        // 주문 생성
        String orderNumber = generateOrderNumber();
        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .totalAmount(0L)
                .build();

        // 주문 상품 추가 및 총액 계산
        long totalAmount = 0L;
        for (CartEntity cart : cartItems) {
            long unitPrice = cart.getSku().getFinalPrice();
            long quantity = cart.getQuantity();

            OrderItem orderItem = OrderItem.builder()
                    .sku(cart.getSku())
                    .quantity(quantity)
                    .price(unitPrice)
                    .build();

            order.addOrderItem(orderItem);
            totalAmount += unitPrice * quantity;
        }

        // 총액 설정 (reflection 없이 새 빌더로)
        Order savedOrder = orderRepository.save(Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build());

        // 주문 상품 재추가
        for (CartEntity cart : cartItems) {
            long unitPrice = cart.getSku().getFinalPrice();
            long quantity = cart.getQuantity();

            OrderItem orderItem = OrderItem.builder()
                    .sku(cart.getSku())
                    .quantity(quantity)
                    .price(unitPrice)
                    .build();

            savedOrder.addOrderItem(orderItem);
        }

        savedOrder = orderRepository.save(savedOrder);
        log.info("주문 생성 완료: orderId={}, orderNumber={}, totalAmount={}",
                savedOrder.getId(), savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        return OrderResponse.from(savedOrder);
    }

    /**
     * 주문 단건 조회
     */
    @Override
    public OrderResponse getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. orderId=" + orderId));

        if (!order.isOwner(userId)) {
            throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다.");
        }

        return OrderResponse.from(order);
    }

    /**
     * 내 주문 목록 조회
     */
    @Override
    public List<OrderResponse> getMyOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * 주문 취소 (결제 전 PENDING 상태에서만 가능)
     */
    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. orderId=" + orderId));

        if (!order.isOwner(userId)) {
            throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다.");
        }

        if (!order.isPending()) {
            throw new IllegalStateException("결제 대기 상태의 주문만 취소할 수 있습니다.");
        }

        order.cancel();
        log.info("주문 취소 완료: orderId={}", orderId);
    }

    /**
     * 주문번호 생성 (UUID 기반, 토스페이먼츠 orderId로 사용)
     * 최대 64자 제한
     */
    private String generateOrderNumber() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
