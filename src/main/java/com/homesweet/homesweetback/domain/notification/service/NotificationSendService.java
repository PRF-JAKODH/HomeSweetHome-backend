package com.homesweet.homesweetback.domain.notification.service;

import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.domain.payload.NotificationPayload;

import java.util.List;
import java.util.Map;

/**
 * 알림 전송 서비스 인터페이스
 * 
 * 다른 Service에서 알림을 보내기 위해 사용하는 서비스
 * @author dogyungkim
 */
public interface NotificationSendService {
    
    /**
     * 알림 전송 단일 사용자 전송 : 템플릿 메시지 전송 (Payload 없음)
     * @param userId 사용자 ID
     * @param templateType 알림 템플릿 타입
     */
    void sendTemplateNotificationToSingleUser(Long userId, NotificationTemplateType templateType);
    
    /**
     * 알림 전송 단일 사용자 전송 : 템플릿 메시지 전송 (타입 안전한 Payload 사용)
     * @param userId 사용자 ID
     * @param templateType 알림 템플릿 타입
     * @param payload 알림 Payload
     */
    void sendTemplateNotificationToSingleUser(Long userId, NotificationTemplateType templateType, NotificationPayload payload);

    /** 
     * 알림 전송 다수 사용자 전송 : 템플릿 메시지 전송 (Payload 없음)
     * @param userIds 사용자 ID 리스트
     * @param templateType 알림 템플릿 타입
     */
    void sendTemplateNotificationToMultipleUsers(List<Long> userIds, NotificationTemplateType templateType);
    
    /** 
     * 알림 전송 다수 사용자 전송 : 템플릿 메시지 전송 (타입 안전한 Payload 사용)
     * @param userIds 사용자 ID 리스트
     * @param templateType 알림 템플릿 타입
     * @param payload 알림 Payload
     */
    void sendTemplateNotificationToMultipleUsers(List<Long> userIds, NotificationTemplateType templateType, NotificationPayload payload);

    /**
     * 알림 전송 단일 사용자 전송 : Custom 메시지 전송
     * @param userId 사용자 ID
     * @param categoryType 알림 카테고리 타입
     * @param title 알림 제목 
     * @param content 알림 내용 
     * @param redirectUrl 알림 리다이렉트 URL
     * @param contextData 알림 컨텍스트 데이터 : 관련 데이터 주문 ID, 결제 ID, 커뮤니티 ID, 정산 ID, 상품 ID, 채팅 ID 등등
     */
    void sendCustomNotificationToSingleUser(Long userId, NotificationCategoryType categoryType, String title, String content, String redirectUrl, Map<String, Object> contextData);

    /**
     * 알림 전송 다수 사용자 전송 : Custom 메시지 전송
     * @param userIds 사용자 ID 리스트
     * @param categoryType 알림 카테고리 타입
     * @param title 알림 제목
     * @param content 알림 내용
     * @param redirectUrl 알림 리다이렉트 URL
     * @param contextData 알림 컨텍스트 데이터 : 관련 데이터 주문 ID, 결제 ID, 커뮤니티 ID, 정산 ID, 상품 ID, 채팅 ID 등등
     */
    void sendCustomNotificationToMultipleUsers(List<Long> userIds, NotificationCategoryType categoryType, String title, String content, String redirectUrl, Map<String, Object> contextData);
}
