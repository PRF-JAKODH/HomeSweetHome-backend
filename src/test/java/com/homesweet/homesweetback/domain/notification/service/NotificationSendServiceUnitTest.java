package com.homesweet.homesweetback.domain.notification.service;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.domain.event.CustomNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.notification.OrderNotification;
import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;
import com.homesweet.homesweetback.domain.notification.service.impl.NotificationSendServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** 
 * Notification Service Impl 단위 테스트
 * 
 * 이벤트 발행만 검증하는 단위 테스트
 * 
 * @author dogyungkim
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSendServiceImpl 테스트")
public class NotificationSendServiceUnitTest {
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    private NotificationSendServiceImpl notificationSendService;

    @BeforeEach
    void setUp() {
        notificationSendService = new NotificationSendServiceImpl(eventPublisher);
    }

    @Test
    @DisplayName("템플릿 알림 이벤트 발행 테스트_단일 사용자_성공")
    void sendTemplateNotificationToSingleUser_Success() {
        // Given
        Long userId = 1L;
        OrderNotification.OrderCompleted notification = OrderNotification.OrderCompleted.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        
        // When
        notificationSendService.sendTemplateNotificationToSingleUser(userId, notification);
        
        // Then - 이벤트가 발행되었는지 확인
        ArgumentCaptor<TemplateNotificationEvent> eventCaptor = ArgumentCaptor.forClass(TemplateNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        TemplateNotificationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userIds()).containsExactly(userId);
        assertThat(capturedEvent.notification()).isEqualTo(notification);
        assertThat(capturedEvent.notification().getEventType()).isEqualTo(NotificationTemplateType.ORDER_COMPLETED);
    }

    @Test
    @DisplayName("템플릿 알림 이벤트 발행 테스트_다수 사용자_성공")
    void sendTemplateNotificationToMultipleUsers_Success() {
        // Given
        List<Long> userIds = List.of(1L, 2L, 3L);
        OrderNotification.OrderCompleted notification = OrderNotification.OrderCompleted.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        
        // When
        notificationSendService.sendTemplateNotificationToMultipleUsers(userIds, notification);
        
        // Then - 이벤트가 발행되었는지 확인
        ArgumentCaptor<TemplateNotificationEvent> eventCaptor = ArgumentCaptor.forClass(TemplateNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        TemplateNotificationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userIds()).isEqualTo(userIds);
        assertThat(capturedEvent.notification()).isEqualTo(notification);
        assertThat(capturedEvent.notification().getEventType()).isEqualTo(NotificationTemplateType.ORDER_COMPLETED);
    }

    @Test
    @DisplayName("커스텀 알림 이벤트 발행 테스트_단일 사용자_성공")
    void sendCustomNotificationToSingleUser_Success() {
        // Given
        Long userId = 1L;
        String title = "시스템 점검";
        String content = "시스템 점검 안내입니다.";
        String redirectUrl = "/maintenance";
        Map<String, Object> contextData = Map.of("maintenanceTime", "2024-01-01 00:00");
        CustomNotification notification = CustomNotification.builder()
            .title(title)
            .content(content)
            .redirectUrl(redirectUrl)
            .contextData(contextData)
            .build();
        
        // When
        notificationSendService.sendCustomNotificationToSingleUser(userId, notification);

        // Then - 이벤트가 발행되었는지 확인
        ArgumentCaptor<CustomNotificationEvent> eventCaptor = ArgumentCaptor.forClass(CustomNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        CustomNotificationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userIds()).containsExactly(userId);
        assertThat(capturedEvent.notification()).isEqualTo(notification);
    }

    @Test
    @DisplayName("커스텀 알림 이벤트 발행 테스트_다수 사용자_성공")
    void sendCustomNotificationToMultipleUsers_Success() {
        // Given
        List<Long> userIds = List.of(1L, 2L, 3L);
        String title = "시스템 점검";
        String content = "시스템 점검 안내입니다.";
        String redirectUrl = "/maintenance";
        Map<String, Object> contextData = Map.of("maintenanceTime", "2024-01-01 00:00");
        CustomNotification notification = CustomNotification.builder()
            .title(title)
            .content(content)
            .redirectUrl(redirectUrl)
            .contextData(contextData)
            .build();
        // When
        notificationSendService.sendCustomNotificationToMultipleUsers(userIds, notification);
        
        // Then - 이벤트가 발행되었는지 확인
        ArgumentCaptor<CustomNotificationEvent> eventCaptor = ArgumentCaptor.forClass(CustomNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        CustomNotificationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userIds()).isEqualTo(userIds);
        assertThat(capturedEvent.notification()).isEqualTo(notification);
    }
}
