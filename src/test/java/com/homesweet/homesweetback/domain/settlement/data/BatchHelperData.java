package com.homesweet.homesweetback.domain.settlement.data;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BatchHelperData {
    public static Grade createGrade() {
        return Grade.builder()
                .grade("VIP")
                .feeRate(new BigDecimal("0.05"))
                .build();
    }


    public static User createSeller(Grade grade) {
        return User.builder()
                .name("seller")
                .email("seller@test.com")
                .phoneNumber("010-1111-2222")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.SELLER)
                .grade(grade)
                .build();
    }

    public static ProductCategoryEntity createCategory() {
        return ProductCategoryEntity.builder()
                .name("가구")
                .depth(1)
                .build();
    }

    public static ProductEntity createProduct(User seller, ProductCategoryEntity category) {
        return ProductEntity.builder()
                .seller(seller)
                .category(category)
                .name("테스트상품")
                .brand("리바트")
                .imageUrl("https://hsweet-bucket-1007.s3.ap-northeast-2.amazonaws.com/product/main/b4e3b77c-0a3e-4474-9e93-1792e2418284_main-image.jpg")
                .basePrice(150000)
                .status(ProductStatus.ON_SALE)
                .build();
    }

    public static SkuEntity createSku(ProductEntity product) {
        return SkuEntity.builder()
                .product(product)
                .priceAdjustment(0)
                .stockQuantity(10L)
                .build();
    }

    public static Order createCompletedOrder(User user, LocalDateTime orderedAt) {
        return Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .user(user)
                .orderStatus(OrderStatus.COMPLETED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .totalAmount(150000L)
                .orderedAt(orderedAt)
                .build();
    }

    public static Order setupFullOrderGraph(Order order, SkuEntity sku) {
        OrderItem item = OrderItem.builder()
                .order(order)
                .sku(sku)
                .quantity(1L)
                .price(150000L)
                .build();

        order.addOrderItem(item);
        return order;
    }
}