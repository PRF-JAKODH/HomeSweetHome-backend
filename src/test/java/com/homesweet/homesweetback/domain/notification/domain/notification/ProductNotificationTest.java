package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

@DisplayName("ProductNotification 테스트")
public class ProductNotificationTest {

    @Test
    @DisplayName("ProductApproved 생성 테스트_성공")
    void testCreateProductApproved() {
        // Given
        ProductNotification.ProductApproved productApproved = ProductNotification.ProductApproved.builder()
            .userName("홍길동")
            .productId("prod-123")
            .productName("상품명")
            .build();

        // Then
        assertThat(productApproved.getUserName()).isEqualTo("홍길동");
        assertThat(productApproved.getProductId()).isEqualTo("prod-123");
        assertThat(productApproved.getProductName()).isEqualTo("상품명");
        assertThat(productApproved.getEventType()).isEqualTo(NotificationTemplateType.PRODUCT_APPROVED);
    }

    @Test
    @DisplayName("ProductApproved 생성 테스트_실패_userName_null")
    void testCreateProductApproved_Failure_UserNameNull() {
        // When & Then
        assertThatThrownBy(() -> ProductNotification.ProductApproved.builder()
            .userName(null)
            .productId("prod-123")
            .productName("상품명")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ProductRejected 생성 테스트_성공")
    void testCreateProductRejected() {
        // Given
        ProductNotification.ProductRejected productRejected = ProductNotification.ProductRejected.builder()
            .userName("홍길동")
            .productId("prod-123")
            .productName("상품명")
            .build();

        // Then
        assertThat(productRejected.getUserName()).isEqualTo("홍길동");
        assertThat(productRejected.getProductId()).isEqualTo("prod-123");
        assertThat(productRejected.getProductName()).isEqualTo("상품명");
        assertThat(productRejected.getEventType()).isEqualTo(NotificationTemplateType.PRODUCT_REJECTED);
    }

    @Test
    @DisplayName("ProductRejected 생성 테스트_실패_productId_blank")
    void testCreateProductRejected_Failure_ProductIdBlank() {
        // When & Then
        assertThatThrownBy(() -> ProductNotification.ProductRejected.builder()
            .userName("홍길동")
            .productId("")
            .productName("상품명")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ProductLowStock 생성 테스트_성공")
    void testCreateProductLowStock() {
        // Given
        ProductNotification.ProductLowStock productLowStock = ProductNotification.ProductLowStock.builder()
            .productId("prod-123")
            .productName("상품명")
            .currentStock("5")
            .build();

        // Then
        assertThat(productLowStock.getProductId()).isEqualTo("prod-123");
        assertThat(productLowStock.getProductName()).isEqualTo("상품명");
        assertThat(productLowStock.getCurrentStock()).isEqualTo("5");
        assertThat(productLowStock.getEventType()).isEqualTo(NotificationTemplateType.PRODUCT_LOW_STOCK);
    }

    @Test
    @DisplayName("ProductLowStock 생성 테스트_실패_currentStock_null")
    void testCreateProductLowStock_Failure_CurrentStockNull() {
        // When & Then
        assertThatThrownBy(() -> ProductNotification.ProductLowStock.builder()
            .productId("prod-123")
            .productName("상품명")
            .currentStock(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NewReview 생성 테스트_성공")
    void testCreateNewReview() {
        // Given
        ProductNotification.NewReview newReview = ProductNotification.NewReview.builder()
            .userName("홍길동")
            .productId("prod-123")
            .productName("상품명")
            .build();

        // Then
        assertThat(newReview.getUserName()).isEqualTo("홍길동");
        assertThat(newReview.getProductId()).isEqualTo("prod-123");
        assertThat(newReview.getProductName()).isEqualTo("상품명");
        assertThat(newReview.getEventType()).isEqualTo(NotificationTemplateType.NEW_REVIEW);
    }

    @Test
    @DisplayName("NewReview 생성 테스트_실패_productName_blank")
    void testCreateNewReview_Failure_ProductNameBlank() {
        // When & Then
        assertThatThrownBy(() -> ProductNotification.NewReview.builder()
            .userName("홍길동")
            .productId("prod-123")
            .productName("")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
}

