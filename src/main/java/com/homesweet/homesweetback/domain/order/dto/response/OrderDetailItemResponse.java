package com.homesweet.homesweetback.domain.order.dto.response;

import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import lombok.Builder;

import java.util.stream.Collectors;

// "주문 상세"에 포함될 개별 상품 DTO
@Builder
public record OrderDetailItemResponse(
        Long orderItemId,
        String productName,
        String productImage,
        String optionName, // 예: "색상: 화이트 / 사이즈: L"
        Long price, // 주문 시점의 할인된 단가
        Long quantity,
        String sellerName,
        Integer shippingFee // 상품별 배송비
) {
    // (정적 팩토리 메서드)
    // OrderItem 엔티티에서 DTO를 생성
    public static OrderDetailItemResponse from(OrderItem orderItem) {
        SkuEntity sku = orderItem.getSku();
        ProductEntity product = sku.getProduct();

        String optionName = sku.getSkuOptions().stream()
                .map(skuOption -> skuOption.getOptionValue().getValue()) // 예: "270mm", "블루"
                .collect(Collectors.joining(" / ")); // "270mm / 블루"

        if (optionName.isEmpty()) {
            optionName = "기본 옵션"; // 옵션이 없는 상품일 경우
        }

        return OrderDetailItemResponse.builder()
                .orderItemId(orderItem.getId())
                .productImage(product.getImageUrl())
                .productName(product.getName())
                .optionName(optionName)
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .sellerName(product.getSeller().getName()) // Product 엔티티가 Seller(User) 정보를 가지고 있어야 함
                .shippingFee(product.getShippingPrice())
                .build();
    }
}