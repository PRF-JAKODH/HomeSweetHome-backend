package com.homesweet.homesweetback.domain.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 템플릿 타입
 * 
 * 📚 사용 가이드:
 * 1. 각 알림 타입별로 필요한 contextData가 명시되어 있습니다.
 * 2. 📋 표시된 필드들은 필수 데이터입니다.
 * 3. 💡 표시된 예시를 참고하여 사용하세요.
 * 4. 모든 데이터는 String 타입으로 전달해주세요.
 * 
 * 🔧 사용 방법:
 * ```java
 * Map<String, Object> context = Map.of(
 *     "userName", "홍길동",
 *     "orderId", "12345",
 *     "productName", "아이폰",
 *     "totalAmount", "1000000"
 * );
 * 
 * NotificationEvent event = new NotificationEvent(
 *     NotificationTemplateType.ORDER_COMPLETED,
 *     userId,
 *     context,
 *     null, null, null  // 커스텀 알림이 아닌 경우 null
 * );
 * ```
 * 
 * @author dogyungkim
 */
@Getter
@RequiredArgsConstructor
public enum NotificationEventType {
    // ==================== 주문 관련 ====================
    /**
     * 주문 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     * 
     * 💡 사용 예시:
     * Map.of("userName", "홍길동", "orderId", "12345")
     */
    ORDER_COMPLETED("주문 완료", NotificationCategoryType.ORDER),
    
    /**
     * 주문 취소 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    ORDER_CANCELLED("주문 취소", NotificationCategoryType.ORDER),
    
    /**
     * 배송 시작 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    ORDER_SHIPPED("배송 시작", NotificationCategoryType.ORDER),
    
    /**
     * 배송 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    ORDER_DELIVERED("배송 완료", NotificationCategoryType.ORDER),
    
    // ==================== 결제 관련 ====================
    /**
     * 결제 성공 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - amount: String - 결제 금액
     */
    PAYMENT_SUCCESS("결제 성공", NotificationCategoryType.PAYMENT),
    
    /**
     * 결제 실패 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    PAYMENT_FAILED("결제 실패", NotificationCategoryType.PAYMENT),
    
    /**
     * 환불 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - amount: String - 환불 금액
     */
    PAYMENT_REFUNDED("환불 완료", NotificationCategoryType.PAYMENT),
    
    // ==================== 커뮤니티 관련 ====================
    /**
     * 새 댓글 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 댓글 작성자 이름
     * - postId: String - 게시글 ID
     * - postTitle: String - 게시글 제목
     */
    NEW_COMMENT("새 댓글", NotificationCategoryType.COMMUNITY),
    
    /**
     * 새 좋아요 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 좋아요 누른 사용자 이름
     * - postId: String - 게시글 ID
     * - postTitle: String - 게시글 제목
     */
    NEW_LIKE("새 좋아요", NotificationCategoryType.COMMUNITY),
    
    /**
     * 새 팔로우 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 팔로우한 사용자 이름
     * - followerId: String - 팔로워 ID
     */
    NEW_FOLLOW("새 팔로우", NotificationCategoryType.COMMUNITY),
    
    // ==================== 정산 관련 ====================
    /**
     * 정산 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - settlementId: String - 정산 ID
     * - amount: String - 정산 금액
     * - settlementName: String - 정산 이름
     */
    SETTLEMENT_COMPLETED("정산 완료", NotificationCategoryType.SETTLEMENT),
    
    /**
     * 정산 실패 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - settlementId: String - 정산 ID
     */
    SETTLEMENT_FAILED("정산 실패", NotificationCategoryType.SETTLEMENT),
    
    // ==================== 상품 관련 ====================
    /**
     * 상품 승인 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     */
    PRODUCT_APPROVED("상품 승인", NotificationCategoryType.PRODUCT),
    
    /**
     * 상품 거부 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     */
    PRODUCT_REJECTED("상품 거부", NotificationCategoryType.PRODUCT),
    
    /**
     * 재고 부족 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     * - currentStock: String - 현재 재고 수량
     */
    PRODUCT_LOW_STOCK("재고 부족", NotificationCategoryType.PRODUCT),
    
    // ==================== 채팅 관련 ====================
    /**
     * 새 메시지 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 메시지 발신자 이름
     * - roomId: String - 채팅방 ID
     * - roomName: String - 채팅방 이름
     * - message: String - 메시지 내용
     */
    NEW_MESSAGE("새 메시지", NotificationCategoryType.CHAT),
    
    /**
     * 채팅방 초대 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 초대한 사용자 이름
     * - roomId: String - 채팅방 ID
     * - roomName: String - 채팅방 이름
     */
    CHAT_ROOM_INVITE("채팅방 초대", NotificationCategoryType.CHAT),
    
    // ==================== 시스템 관련 ====================
    /**
     * 시스템 점검 알림
     * 
     * 📋 필요한 contextData:
     * - maintenanceTime: String - 점검 시간
     */
    SYSTEM_MAINTENANCE("시스템 점검", NotificationCategoryType.SYSTEM),
    
    /**
     * 시스템 업데이트 알림
     * 
     * 📋 필요한 contextData:
     * - version: String - 업데이트 버전
     * - updateFeatures: String - 업데이트 기능 목록
     */
    SYSTEM_UPDATE("시스템 업데이트", NotificationCategoryType.SYSTEM),
    
    // ==================== 프로모션 관련 ====================
    /**
     * 프로모션 시작 알림
     * 
     * 📋 필요한 contextData:
     * - promotionName: String - 프로모션 이름
     */
    PROMOTION_START("프로모션 시작", NotificationCategoryType.PROMOTION),
    
    /**
     * 프로모션 종료 알림
     * 
     * 📋 필요한 contextData:
     * - promotionName: String - 프로모션 이름
     */
    PROMOTION_END("프로모션 종료", NotificationCategoryType.PROMOTION),
    
    // ==================== 커스텀 알림 ====================
    /**
     * 커스텀 알림
     * 
     * 📋 필요한 contextData:
     * - 사용자 정의 (title, content, redirectUrl 직접 지정)
     * 
     * 💡 사용 예시:
     * NotificationEvent event = new NotificationEvent(
     *     NotificationTemplateType.CUSTOM, 
     *     userId, 
     *     context, 
     *     "긴급 공지", 
     *     "시스템 점검 안내", 
     *     "app://maintenance"
     * );
     */
    CUSTOM("커스텀 알림", NotificationCategoryType.CUSTOM);

    private final String description;
    private final NotificationCategoryType categoryType;

    public static NotificationEventType fromCode(String code) {
        try {
            return NotificationEventType.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown notification template type: " + code);
        }
    }
}