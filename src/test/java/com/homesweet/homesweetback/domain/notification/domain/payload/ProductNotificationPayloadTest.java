package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductNotificationPayload 테스트")
class ProductNotificationPayloadTest {
    
    @Nested
    @DisplayName("ProductApprovedPayload 테스트")
    class ProductApprovedPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            String productId = "123";
            String productName = "테스트 상품";
            
            ProductNotificationPayload.ProductApprovedPayload payload = 
                ProductNotificationPayload.ProductApprovedPayload.builder()
                    .userName(userName)
                    .productId(productId)
                    .productName(productName)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("productId")).isEqualTo(productId);
            assertThat(result.get("productName")).isEqualTo(productName);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            ProductNotificationPayload.ProductApprovedPayload payload = 
                ProductNotificationPayload.ProductApprovedPayload.builder()
                    .userName("홍길동")
                    .productId("123")
                    .productName("테스트 상품")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.PRODUCT_APPROVED);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            ProductNotificationPayload.ProductApprovedPayload payload = 
                ProductNotificationPayload.ProductApprovedPayload.builder()
                    .userName(null)
                    .productId("123")
                    .productName("테스트 상품")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.PRODUCT_APPROVED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("validate() - productId가 null일 때 예외 발생")
        void testValidate_ProductIdNull() {
            // Given
            ProductNotificationPayload.ProductApprovedPayload payload = 
                ProductNotificationPayload.ProductApprovedPayload.builder()
                    .userName("홍길동")
                    .productId(null)
                    .productName("테스트 상품")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.PRODUCT_APPROVED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productId is required");
        }
        
        @Test
        @DisplayName("validate() - productName이 null일 때 예외 발생")
        void testValidate_ProductNameNull() {
            // Given
            ProductNotificationPayload.ProductApprovedPayload payload = 
                ProductNotificationPayload.ProductApprovedPayload.builder()
                    .userName("홍길동")
                    .productId("123")
                    .productName(null)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.PRODUCT_APPROVED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productName is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = ProductNotificationPayload.ProductApprovedPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.PRODUCT_APPROVED);
        }
    }
    
    @Nested
    @DisplayName("ProductRejectedPayload 테스트")
    class ProductRejectedPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            String productId = "123";
            String productName = "테스트 상품";
            
            ProductNotificationPayload.ProductRejectedPayload payload = 
                ProductNotificationPayload.ProductRejectedPayload.builder()
                    .userName(userName)
                    .productId(productId)
                    .productName(productName)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("productId")).isEqualTo(productId);
            assertThat(result.get("productName")).isEqualTo(productName);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            ProductNotificationPayload.ProductRejectedPayload payload = 
                ProductNotificationPayload.ProductRejectedPayload.builder()
                    .userName("홍길동")
                    .productId("123")
                    .productName("테스트 상품")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.PRODUCT_REJECTED);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            ProductNotificationPayload.ProductRejectedPayload payload = 
                ProductNotificationPayload.ProductRejectedPayload.builder()
                    .userName(null)
                    .productId("123")
                    .productName("테스트 상품")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.PRODUCT_REJECTED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = ProductNotificationPayload.ProductRejectedPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.PRODUCT_REJECTED);
        }
    }
    
    @Nested
    @DisplayName("ProductLowStockPayload 테스트")
    class ProductLowStockPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String productId = "123";
            String productName = "테스트 상품";
            String currentStock = "5";
            
            ProductNotificationPayload.ProductLowStockPayload payload = 
                ProductNotificationPayload.ProductLowStockPayload.builder()
                    .productId(productId)
                    .productName(productName)
                    .currentStock(currentStock)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("productId")).isEqualTo(productId);
            assertThat(result.get("productName")).isEqualTo(productName);
            assertThat(result.get("currentStock")).isEqualTo(currentStock);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            ProductNotificationPayload.ProductLowStockPayload payload = 
                ProductNotificationPayload.ProductLowStockPayload.builder()
                    .productId("123")
                    .productName("테스트 상품")
                    .currentStock("5")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.PRODUCT_LOW_STOCK);
        }
        
        @Test
        @DisplayName("validate() - productId가 null일 때 예외 발생")
        void testValidate_ProductIdNull() {
            // Given
            ProductNotificationPayload.ProductLowStockPayload payload = 
                ProductNotificationPayload.ProductLowStockPayload.builder()
                    .productId(null)
                    .productName("테스트 상품")
                    .currentStock("5")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.PRODUCT_LOW_STOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productId is required");
        }
        
        @Test
        @DisplayName("validate() - currentStock이 null일 때 예외 발생")
        void testValidate_CurrentStockNull() {
            // Given
            ProductNotificationPayload.ProductLowStockPayload payload = 
                ProductNotificationPayload.ProductLowStockPayload.builder()
                    .productId("123")
                    .productName("테스트 상품")
                    .currentStock(null)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.PRODUCT_LOW_STOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentStock is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = ProductNotificationPayload.ProductLowStockPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.PRODUCT_LOW_STOCK);
        }
    }
    
    @Nested
    @DisplayName("NewReviewPayload 테스트")
    class NewReviewPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            String productId = "123";
            String productName = "테스트 상품";
            
            ProductNotificationPayload.NewReviewPayload payload = 
                ProductNotificationPayload.NewReviewPayload.builder()
                    .userName(userName)
                    .productId(productId)
                    .productName(productName)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("productId")).isEqualTo(productId);
            assertThat(result.get("productName")).isEqualTo(productName);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            ProductNotificationPayload.NewReviewPayload payload = 
                ProductNotificationPayload.NewReviewPayload.builder()
                    .userName("홍길동")
                    .productId("123")
                    .productName("테스트 상품")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.NEW_REVIEW);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            ProductNotificationPayload.NewReviewPayload payload = 
                ProductNotificationPayload.NewReviewPayload.builder()
                    .userName(null)
                    .productId("123")
                    .productName("테스트 상품")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_REVIEW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = ProductNotificationPayload.NewReviewPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.NEW_REVIEW);
        }
    }
}
