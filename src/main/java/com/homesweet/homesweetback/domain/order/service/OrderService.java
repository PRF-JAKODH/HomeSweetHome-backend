package com.homesweet.homesweetback.domain.order.service;

// --- DTO Imports ---

import com.homesweet.homesweetback.common.exception.OrderNotFoundException;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingOrder;
import com.homesweet.homesweetback.domain.order.dto.request.CreateOrderRequest;
import com.homesweet.homesweetback.domain.order.dto.response.MyOrderItemResponse;
import com.homesweet.homesweetback.domain.order.dto.response.OrderDetailResponse;
import com.homesweet.homesweetback.domain.order.dto.response.OrderReadyResponse;

// --- Entity Imports ---
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.order.entity.*;
import com.homesweet.homesweetback.domain.order.repository.PaymentRepository;
import com.homesweet.homesweetback.domain.product.product.command.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;

// --- Repository Imports ---
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.ProductJPARepository;

// --- Exception Imports ---
import jakarta.persistence.EntityNotFoundException;

// --- Spring & Java Imports ---
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
//@Transactional(readOnly = true)
public class OrderService {

    // --- 주문 생성에 필요한 Repository만 주입 ---
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SkuJPARepository skuJPARepository;
    private final PaymentRepository paymentRepository;
    private final ProductJPARepository productJPARepository;

    // 레디스
    private final RedisStockService redisStockService;


    /**
     * API 1: 주문 생성 (결제 준비)
     * (재고 검증 로직은 PaymentService로 이동)
     */
//    @Transactional
    public OrderReadyResponse createOrder(CreateOrderRequest dto, Long userId) {

        // ------[1]DB 조회 구간------
        // 사용자(User) 조회 - 레포지토리 자체적으로 트랜잭션이 있어서 안전함.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 상품(Sku) 일괄 조회 (N+1 방지 및 DB 접근 최소화)
        // DTO에서 skuId 목록만 추출
        List<Long> skuIds = dto.orderItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::skuId)
                .toList();

        // (DB에서 한 번에서 가져옴 - findAllById 사용)
        List<SkuEntity> skus = skuJPARepository.findAllById(skuIds);

        // 검증: 요청한 개수와 조회된 개수가 맞는지
        if (skus.size() != skuIds.size()) {
            throw new EntityNotFoundException("일부 상품을 찾을 수 없습니다.");
        }

        // 편리한 사용을 위해 Map으로 변환: SkuId -> SkuEntity
        Map<Long, SkuEntity> skuMap = skus.stream()
                .collect(Collectors.toMap(SkuEntity::getId, sku -> sku));

        //------[2] 비즈니스 로직(메모리 연산)------
        List<PendingOrder.PendingOrderItem> pendingItems = new ArrayList<>();
        long totalAmount = 0L;
        int totalShippingPrice = 0;
        Set<Long> processedProductIds = new HashSet<>();

        for (CreateOrderRequest.OrderItemRequest itemDto : dto.orderItems()){
            // 메모리에 올려둔 Map에서 꺼냄 (DB 조회 X)
            SkuEntity sku = skuMap.get(itemDto.skuId());
            ProductEntity product = sku.getProduct();

            // 상태 검증
            if (product.getStatus() != ProductStatus.ON_SALE) {
                throw new RuntimeException("판매 중인 상품이 아닙니다. 상품 ID: " + product.getId());
            }

            // 가격 계산
            long discountedPrice = sku.getFinalPrice(); // 메모리 연산
            totalAmount += sku.calculateTotalPrice(itemDto.quantity()); // 메모리 연산

            // Redis 재고 차감 (네트워크 I/O) -> 트랜잭션 밖이라 안전 -> 이 부분에서 시간이 걸려도 DB 커넥션은 안 잡고 있음
            redisStockService.decreaseStock(itemDto.skuId(), itemDto.quantity());

            // DTO 생성
            pendingItems.add(new PendingOrder.PendingOrderItem(
                    sku.getId(),
                    itemDto.quantity(),
                    discountedPrice
            ));
        }

        // 총액 합산
        totalAmount += totalShippingPrice;
        String newOrderNumber = this.generateOrderNumber();

        //------[3]Redis 저장------

        PendingOrder pendingOrder = new PendingOrder(
                userId,
                newOrderNumber,
                totalAmount,
                pendingItems,
                dto.recipientName(),
                dto.recipientPhone(),
                dto.shippingAddress(),
                dto.shippingRequest()
        );

        redisStockService.pushPendingOrder(pendingOrder);
        redisStockService.cacheOrder(pendingOrder);

        // 응답 반환
        return new OrderReadyResponse(
                null,
                newOrderNumber,
                "Processing...",
                "", "", new ArrayList<>(),
                totalAmount,
                totalShippingPrice
        );

    }

    @Transactional(readOnly = true)
    public List<MyOrderItemResponse> getMyOrders(Long userId) {
        // 1. 사용자(User) 조회
        //TODO: userRepo를 참조해서 order에서 검증하는게 맞나? v

        // 2. 주문 목록 조회 (N+1 방지용 쿼리 사용)
        List<Order> orders = orderRepository.findAllByUserWithDetails(userId);

        // 3. MyOrderItemResponse DTO 리스트로 변환
        return orders.stream().map(order -> {

                    String productName = "주문 항목 없음";
                    String imageUrl = ""; // 기본 이미지 URL

                    List<OrderItem> orderItems = order.getOrderItems();

                    if (orderItems != null && !orderItems.isEmpty()) {
                        // 3-1. 대표 상품(첫 번째 OrderItem) 정보 가져오기
                        OrderItem representativeItem = orderItems.get(0);
                        ProductEntity product = representativeItem.getSku().getProduct();

                        imageUrl = product.getImageUrl(); // (결정 1) 이미지는 대표 상품 1개

                        // 3-2. (수정) 상품명: "A상품 외 N건" 형식으로 조합
                        String firstProductName = product.getName();
                        int otherItemsCount = orderItems.size() - 1;

                        if (otherItemsCount > 0) {
                            productName = String.format("%s 외 %d건", firstProductName, otherItemsCount);
                        } else {
                            productName = firstProductName;
                        }
                    }

                    // 3-3. (수정) 가격: 주문의 '최종 결제 금액' 사용
                    Long price = order.getTotalAmount(); // (결정 3)

                    // 3-4. DTO 생성
                    return MyOrderItemResponse.builder()
                            .orderId(order.getId())
                            .orderNumber(order.getOrderNumber())
                            .orderDate(order.getOrderedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .productName(productName) // "A상품 외 1건"
                            .imageUrl(imageUrl)       // "A상품 이미지"
                            .price(price)             // "주문 총액"
                            .orderStatus(order.getOrderStatus().name())
                            .deliveryStatus(order.getDeliveryStatus().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId, Long userId) {

        // 1. 주문 조회 (N+1 방지를 위해 모든 연관 엔티티를 fetch join)
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderId));

        // 2. (보안) 주문자 본인 확인
        //TODO: 캡슐화 적용!! v
        order.validateOwner(userId);

        // 3. 결제 정보 조회
        // (결제가 PENDING 상태일 경우 Payment가 없을 수 있으므로 orElse(null) 처리)
        Payment payment = paymentRepository.findByOrder(order)
                .orElse(null); // 결제 대기중인 주문은 payment가 null일 수 있음

        // 4. 총 배송비 계산 (productId 기준 중복 제거)
        Integer totalShippingPrice = order.getOrderItems().stream()
                .map(item -> item.getSku().getProduct()) // OrderItem -> Product
                .map(ProductEntity::getId)               // Product -> Product ID
                .distinct()                              // [핵심] Product ID 중복 제거
                .mapToInt(productId -> order.getOrderItems().stream()
                        .filter(oi -> oi.getSku().getProduct().getId().equals(productId))
                        .findFirst() // 각 Product ID당 첫 번째 아이템만 찾음
                        .map(oi -> oi.getSku().getProduct().getShippingPrice()) // 그 아이템의 배송비
                        .orElse(0))
                .sum();

        // 5. DTO로 변환
        return OrderDetailResponse.of(order, payment, order.getUser(), totalShippingPrice);
    }

    private String generateOrderNumber() {
        String uuid = UUID.randomUUID().toString();
        String cleanUuid = uuid.replace("-", "");
        return "ORD-" + cleanUuid.toUpperCase();
    }
}