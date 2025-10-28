package com.homesweet.homesweetback.domain.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
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
        notificationSendService.sendTemplateNotificationToSingleUser(1L, NotificationTemplateType.ORDER_COMPLETED, payload);
    }

    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe(@AuthenticationPrincipal User user) {
        log.info("SSE 연결 요청: userId={}", user.getId());
        return sseService.subscribe(user.getId());
    }
    
    /**
     * 사용자의 알림 목록 조회 (최대 20개)
     */
    @GetMapping
    public ResponseEntity<List<PushNotificationDTO>> getNotifications(@AuthenticationPrincipal User user) {
        log.info("알림 목록 조회: userId={}", user.getId());
        List<PushNotificationDTO> notifications = notificationAPIService.getAllNotifications(user.getId());
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * 알림 읽음 처리 (단일 및 여러 개 모두 처리)
     * 
     * RequestBody 예시:
     * - 단일: [1]
     * - 여러 개: [1, 2, 3]
     */
    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @RequestBody List<Long> notificationIds) {
        log.info("알림 읽음 처리: userId={}, notificationIds={}", user.getId(), notificationIds);
        notificationAPIService.markAsRead(user.getId(), notificationIds);
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
            @AuthenticationPrincipal User user,
            @RequestBody List<Long> notificationIds) {
        log.info("알림 삭제 처리: userId={}, notificationIds={}", user.getId(), notificationIds);
        notificationAPIService.markAsDeleted(user.getId(), notificationIds);
        return ResponseEntity.ok().build();
    }
}
