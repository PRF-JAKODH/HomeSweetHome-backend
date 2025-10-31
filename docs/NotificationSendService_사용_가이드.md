# NotificationSendService 사용 가이드

다른 팀에서 알림을 보내야 할 때 이 서비스를 사용하세요. 

## 📋 목차
1. [빠른 시작](#빠른-시작)
2. [템플릿 기반 알림 전송](#템플릿-기반-알림-전송)
3. [커스텀 알림 전송](#커스텀-알림-전송)
4. [사용 가능한 알림 타입](#사용-가능한-알림-타입)
5. [Payload 클래스 사용법](#payload-클래스-사용법)
6. [주의사항](#주의사항)

---

## 🚀 빠른 시작

### 1. NotificationSendService 주입
```java
@Service
@RequiredArgsConstructor
public class YourService {
    private final NotificationSendService notificationSendService;
    
    // ... 나머지 코드
}
```

### 2. 간단한 예시: 주문 완료 알림
```java
// 주문 완료 시 알림 전송
public void completeOrder(Long userId, String orderId, String userName) {
    // Payload 생성
    OrderNotificationPayload.OrderCompletedPayload payload = 
        OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName(userName)
            .orderId(orderId)
            .build();
    
    // 알림 전송
    notificationSendService.sendTemplateNotificationToSingleUser(
        userId, 
        NotificationEventType.ORDER_COMPLETED, 
        payload
    );
}
```

---

## 📨 템플릿 기반 알림 전송

템플릿 기반 알림은 DB에 저장된 템플릿을 사용하여 알림을 전송합니다. 
타입 안전한 Payload를 사용하여 실수를 방지할 수 있습니다.

### 단일 사용자에게 전송

#### Payload 사용 (권장)
```java
// 주문 완료 알림
OrderNotificationPayload.OrderCompletedPayload payload = 
    OrderNotificationPayload.OrderCompletedPayload.builder()
        .userName("홍길동")
        .orderId("12345")
        .build();

notificationSendService.sendTemplateNotificationToSingleUser(
    userId, 
    NotificationEventType.ORDER_COMPLETED, 
    payload
);
```

#### Payload 없이 전송
```java
// Payload 없이 템플릿만 사용 (contextData 없음)
notificationSendService.sendTemplateNotificationToSingleUser(
    userId, 
    NotificationEventType.ORDER_COMPLETED
);
```

### 여러 사용자에게 전송

#### Payload 사용 (권장)
```java
List<Long> userIds = List.of(1L, 2L, 3L);

OrderNotificationPayload.OrderCompletedPayload payload = 
    OrderNotificationPayload.OrderCompletedPayload.builder()
        .userName("홍길동")
        .orderId("12345")
        .build();

notificationSendService.sendTemplateNotificationToMultipleUsers(
    userIds, 
    NotificationEventType.ORDER_COMPLETED, 
    payload
);
```

#### Payload 없이 전송
```java
List<Long> userIds = List.of(1L, 2L, 3L);

notificationSendService.sendTemplateNotificationToMultipleUsers(
    userIds, 
    NotificationEventType.ORDER_COMPLETED
);
```

---

## 🎨 커스텀 알림 전송

템플릿이 없는 경우 커스텀 알림을 사용할 수 있습니다.

### 단일 사용자에게 전송
```java
Map<String, Object> contextData = Map.of(
    "orderId", "12345",
    "amount", "50000"
);

notificationSendService.sendCustomNotificationToSingleUser(
    userId,
    NotificationCategoryType.ORDER,
    "긴급 공지",
    "주문이 완료되었습니다.",
    "app://order/12345",
    contextData
);
```

### 여러 사용자에게 전송
```java
List<Long> userIds = List.of(1L, 2L, 3L);
Map<String, Object> contextData = Map.of(
    "promotionId", "PROMO_001"
);

notificationSendService.sendCustomNotificationToMultipleUsers(
    userIds,
    NotificationCategoryType.PROMOTION,
    "특가 이벤트",
    "오늘만 특별 할인!",
    "app://promotion/PROMO_001",
    contextData
);
```

---

## 📚 사용 가능한 알림 타입
### | 필요한 알림 타입이 있다면 꼭 알려주세요!! |

### 주문 관련 (`NotificationEventType`)
- `ORDER_COMPLETED` - 주문 완료
- `ORDER_CANCELLED` - 주문 취소
- `ORDER_SHIPPED` - 배송 시작
- `ORDER_DELIVERED` - 배송 완료

### 결제 관련 (`NotificationEventType`)
- `PAYMENT_SUCCESS` - 결제 성공
- `PAYMENT_FAILED` - 결제 실패
- `PAYMENT_REFUNDED` - 환불 완료

### 커뮤니티 관련 (`NotificationEventType`)
- `NEW_COMMENT` - 새 댓글
- `NEW_LIKE` - 새 좋아요
- `NEW_FOLLOW` - 새 팔로우

### 상품 관련 (`NotificationEventType`)
- `PRODUCT_APPROVED` - 상품 승인
- `PRODUCT_REJECTED` - 상품 거부
- `PRODUCT_LOW_STOCK` - 재고 부족

### 채팅 관련 (`NotificationEventType`)
- `NEW_MESSAGE` - 새 메시지
- `CHAT_ROOM_INVITE` - 채팅방 초대

### 시스템 관련 (`NotificationEventType`)
- `SYSTEM_MAINTENANCE` - 시스템 점검
- `SYSTEM_UPDATE` - 시스템 업데이트

### 프로모션 관련 (`NotificationEventType`)
- `PROMOTION_START` - 프로모션 시작
- `PROMOTION_END` - 프로모션 종료

> 💡 **더 많은 타입 확인**: `NotificationEventType` ENUM 클래스에서 모든 타입과 필요한 contextData를 확인할 수 있습니다.

---

## 📦 Payload 클래스 사용법

### OrderNotificationPayload 예시

```java
// 주문 완료
OrderNotificationPayload.OrderCompletedPayload orderCompleted = 
    OrderNotificationPayload.OrderCompletedPayload.builder()
        .userName("홍길동")
        .orderId("12345")
        .build();

// 주문 취소
OrderNotificationPayload.OrderCancelledPayload orderCancelled = 
    OrderNotificationPayload.OrderCancelledPayload.builder()
        .userName("홍길동")
        .orderId("12345")
        .build();

// 배송 시작
OrderNotificationPayload.OrderShippedPayload orderShipped = 
    OrderNotificationPayload.OrderShippedPayload.builder()
        .userName("홍길동")
        .orderId("12345")
        .build();

// 배송 완료
OrderNotificationPayload.OrderDeliveredPayload orderDelivered = 
    OrderNotificationPayload.OrderDeliveredPayload.builder()
        .userName("홍길동")
        .orderId("12345")
        .build();
```

### ProductNotificationPayload 예시

```java
// 상품 승인
ProductNotificationPayload.ProductApprovedPayload productApproved = 
    ProductNotificationPayload.ProductApprovedPayload.builder()
        .userName("홍길동")
        .productId("P001")
        .productName("아이폰 15")
        .build();

// 상품 거부
ProductNotificationPayload.ProductRejectedPayload productRejected = 
    ProductNotificationPayload.ProductRejectedPayload.builder()
        .userName("홍길동")
        .productId("P001")
        .productName("아이폰 15")
        .build();

// 재고 부족
ProductNotificationPayload.ProductLowStockPayload productLowStock = 
    ProductNotificationPayload.ProductLowStockPayload.builder()
        .userName("홍길동")
        .productId("P001")
        .productName("아이폰 15")
        .currentStock("5")
        .build();
```

### 💡 Payload의 장점

1. **타입 안전성**: 컴파일 타임에 필수 필드를 확인할 수 있습니다.
2. **자동 검증**: Payload와 EventType이 일치하지 않으면 예외가 발생합니다.
3. **명확한 필드**: 각 Payload 클래스에 필요한 필드가 명시되어 있습니다.

---

## ⚠️ 주의사항

### 1. EventType과 Payload 매칭
각 Payload는 특정 EventType과만 사용할 수 있습니다.

```java
// ✅ 올바른 사용
OrderNotificationPayload.OrderCompletedPayload payload = ...;
notificationSendService.sendTemplateNotificationToSingleUser(
    userId, 
    NotificationEventType.ORDER_COMPLETED,  // 올바른 EventType
    payload
);

// ❌ 잘못된 사용 - 예외 발생
OrderNotificationPayload.OrderCompletedPayload payload = ...;
notificationSendService.sendTemplateNotificationToSingleUser(
    userId, 
    NotificationEventType.PRODUCT_APPROVED,  // 잘못된 EventType
    payload
);
```

### 2. 필수 필드 확인
각 Payload의 필수 필드를 반드시 채워야 합니다.

```java
// ❌ 잘못된 사용 - userName 누락
OrderNotificationPayload.OrderCompletedPayload payload = 
    OrderNotificationPayload.OrderCompletedPayload.builder()
        // userName 누락!
        .orderId("12345")
        .build();
// → IllegalArgumentException 발생

// ✅ 올바른 사용
OrderNotificationPayload.OrderCompletedPayload payload = 
    OrderNotificationPayload.OrderCompletedPayload.builder()
        .userName("홍길동")
        .orderId("12345")
        .build();
```

### 3. 사용자 ID 유효성
전송할 `userId`가 실제로 존재하는 사용자인지 확인하세요.

---

## 🎯 실전 예시

### 예시 1: 주문 서비스에서 주문 완료 알림
```java
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final NotificationSendService notificationSendService;
    
    public void completeOrder(Long orderId) {
        // 주문 완료 로직...
        Order order = orderRepository.findById(orderId)
            .orElseThrow();
        
        // 알림 전송
        OrderNotificationPayload.OrderCompletedPayload payload = 
            OrderNotificationPayload.OrderCompletedPayload.builder()
                .userName(order.getUser().getName())
                .orderId(orderId.toString())
                .build();
        
        notificationSendService.sendTemplateNotificationToSingleUser(
            order.getUser().getId(),
            NotificationEventType.ORDER_COMPLETED,
            payload
        );
    }
}
```

### 예시 2: 상품 관리 서비스에서 재고 부족 알림
```java
@Service
@RequiredArgsConstructor
public class ProductManagementService {
    private final NotificationSendService notificationSendService;
    
    public void checkLowStock(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow();
        
        if (product.getStock() < 10) {
            // 관리자들에게 알림 전송
            List<Long> adminIds = getAdminUserIds();
            
            ProductNotificationPayload.ProductLowStockPayload payload = 
                ProductNotificationPayload.ProductLowStockPayload.builder()
                    .userName("관리자")
                    .productId(productId.toString())
                    .productName(product.getName())
                    .currentStock(String.valueOf(product.getStock()))
                    .build();
            
            notificationSendService.sendTemplateNotificationToMultipleUsers(
                adminIds,
                NotificationEventType.PRODUCT_LOW_STOCK,
                payload
            );
        }
    }
}
```

### 예시 3: 커스텀 알림 - 긴급 공지
```java
@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final NotificationSendService notificationSendService;
    
    public void sendUrgentAnnouncement(String title, String content) {
        // 모든 활성 사용자에게 전송
        List<Long> activeUserIds = userRepository.findActiveUserIds();
        
        Map<String, Object> contextData = Map.of(
            "announcementId", UUID.randomUUID().toString(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        notificationSendService.sendCustomNotificationToMultipleUsers(
            activeUserIds,
            NotificationCategoryType.SYSTEM,
            title,
            content,
            "app://announcement",
            contextData
        );
    }
}
```

---

## 🔍 문제 해결

### 문제: `NotificationException` 발생
**원인**: Payload와 EventType이 일치하지 않음  
**해결**: 올바른 EventType을 사용하거나, 해당 EventType에 맞는 Payload를 사용하세요.

### 문제: `IllegalArgumentException` 발생
**원인**: Payload의 필수 필드가 누락됨  
**해결**: Payload 빌더에서 모든 필수 필드를 설정하세요.

### 문제: 알림이 전송되지 않음
**원인**: 
- 사용자가 SSE 연결을 하지 않음
- 템플릿이 DB에 없음
- userId가 잘못됨

**해결**: 
- 클라이언트에서 `/api/v1/notifications/subscribe` 엔드포인트로 SSE 연결 확인
- 해당 EventType의 템플릿이 DB에 등록되어 있는지 확인
- userId 유효성 확인

---

## 📞 추가 도움말

더 자세한 정보가 필요하다면:
1. `NotificationEventType` ENUM 클래스를 확인하세요 (각 타입별 필요한 contextData 명시)
2. `NotificationSendService` 인터페이스 주석을 확인하세요
3. 테스트 코드 (`NotificationSendServiceTest`)를 참고하세요

---

**작성자**: dogyungkim  
**최종 업데이트**: 2025-10-30

