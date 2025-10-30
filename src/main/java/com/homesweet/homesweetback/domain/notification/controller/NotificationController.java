package com.homesweet.homesweetback.domain.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.domain.payload.OrderNotificationPayload;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import com.homesweet.homesweetback.domain.notification.service.SseService;
import com.homesweet.homesweetback.domain.notification.service.impl.NotificationAPIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final SseService sseService;
    private final NotificationAPIService notificationAPIService;

    private final NotificationSendService notificationSendService;

    /**
     * SSE 알림 테스트
     * 
    **/
    @GetMapping("/test")
    public void testMessage(){
        var payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("test")
            .orderId("12345")
            .build();
        notificationSendService.sendTemplateNotificationToSingleUser(12L, NotificationEventType.ORDER_COMPLETED, payload);
    }

    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe(@AuthenticationPrincipal OAuth2UserPrincipal principal) {
        log.info("SSE 연결 요청: userId={}", principal.getUserId());
        return sseService.subscribe(principal.getUserId());
    }
    
    /**
     * 사용자의 알림 목록 조회 (최대 20개)
     */
    @GetMapping
    public ResponseEntity<List<PushNotificationDTO>> getNotifications(@AuthenticationPrincipal OAuth2UserPrincipal principal) {
        log.info("알림 목록 조회: userId={}", principal.getUserId());
        List<PushNotificationDTO> notifications = notificationAPIService.getAllNotifications(principal.getUserId());
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * 알림 읽음 처리 (단일 및 여러 개 모두 처리)
     * 
     * RequestBody 예시:
     * - 단일: [1]
     * - 여러 개: [1, 2, 3]
     */
    @PatchMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @RequestBody List<Long> notificationIds) {
        log.info("알림 읽음 처리: userId={}, notificationIds={}", principal.getUserId(), notificationIds);
        notificationAPIService.markAsRead(principal.getUserId(), notificationIds);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 알림 삭제 처리 (단일 및 여러 개 모두 처리)
     * 
     * RequestBody 예시:
     * - 단일: [1]
     * - 여러 개: [1, 2, 3]
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @RequestBody List<Long> notificationIds) {
        log.info("알림 삭제 처리: userId={}, notificationIds={}", principal.getUserId(), notificationIds);
        notificationAPIService.markAsDeleted(principal.getUserId(), notificationIds);
        return ResponseEntity.ok().build();
    }
}
