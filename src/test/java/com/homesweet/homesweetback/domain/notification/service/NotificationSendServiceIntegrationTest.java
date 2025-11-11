package com.homesweet.homesweetback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.domain.payload.OrderNotificationPayload;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.NotificationCategoryRepository;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;


@SpringBootTest
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("NotificationSendServiceIntegrationTest 테스트")
public class NotificationSendServiceIntegrationTest {

    @Autowired
    private NotificationSendService notificationSendService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private NotificationCategoryRepository notificationCategoryRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @MockitoBean
    private SseService sseService;

    private NotificationCategory testCategory;
    private User testUser;
    private NotificationTemplate testTemplate;

    @BeforeAll
    void setUp() {
        //sservice mock 설정
        doNothing().when(sseService).sendNotification(anyLong(), anyString());
        // 테스트 사용자 생성
        testUser = userRepository.save(createTestUser());
        // 테스트 카테고리 생성
        testCategory = notificationCategoryRepository.save(NotificationCategory.builder()
                                                                                .categoryType(NotificationCategoryType.ORDER)
                                                                                .build());
        // 테스트 카테고리 생성
        notificationCategoryRepository.save(NotificationCategory.builder()
                                                                                .categoryType(NotificationCategoryType.CUSTOM)
                                                                                .build());
        // 테스트 템플릿 생성
        testTemplate = notificationTemplateRepository.save(createTestNotificationTemplate());
        // 테스트 카테고리 데이터베이스 설정 -> Category는 항상 데이터베이스에 있다고 가정
        setUpNotificationCategoryOnDatabase();
    }

    @AfterEach
    void tearDown() {
        userNotificationRepository.deleteAll();
    }

    @Test
    @DisplayName("템플릿 알림 전송 테스트_성공")
    void sendTemplateNotification_Success() {
        // Given
        Long userId = testUser.getId();
        NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        // When
        notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload);
        UserNotification userNotification = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId).stream().findFirst().orElseThrow();

        // Then
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.getUser().getId()).isEqualTo(userId);
        assertThat(userNotification.getTemplate().getId()).isEqualTo(testTemplate.getId());
        assertThat(userNotification.getContextData()).isEqualTo(payload.toMap());
        assertThat(userNotification.getIsRead()).isFalse();
        assertThat(userNotification.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("템플릿 알림 전송 테스트_실패_이벤트 타입과 payload가 일치하지 않음")
    void sendTemplateNotification_Fail_EventTypeAndPayloadNotMatch() {
        // Given
        Long userId = testUser.getId();
        NotificationEventType eventType = NotificationEventType.ORDER_CANCELLED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        // When
        assertThatThrownBy(() -> notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload))
            .isInstanceOf(NotificationException.class)
            .hasMessageContaining(ErrorCode.NOTIFICATION_EVENT_TYPE_MISMATCH.getMessage());
    }
     
    @Test
    @DisplayName("템플릿 알림 전송 테스트_실패_payload 검증 실패")
    void sendTemplateNotification_Fail_PayloadValidationFailed() {
        // Given
        Long userId = testUser.getId();
        NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName(null)
            .orderId(null)
            .build();
        // When
        assertThatThrownBy(() -> notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userName is required for ORDER_COMPLETED notification");
    }

    @Test
    @DisplayName("템플릿 알림 전송 테스트_실패_사용자 존재하지 않음")
    void sendTemplateNotification_Fail_UserNotFound() {
        // Given
        Long userId = 9999999999L;
        NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        // When
        // Then
        assertThatThrownBy(() -> notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("템플릿 알림 전송 테스트_실패_템플릿이 DB에 없음")
    void sendTemplateNotification_Fail_TemplateNotFound() {
        // Given
        Long userId = testUser.getId();
        NotificationEventType eventType = NotificationEventType.ORDER_SHIPPED; // DB에 없는 템플릿
        OrderNotificationPayload.OrderShippedPayload payload = OrderNotificationPayload.OrderShippedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        // When & Then
        assertThatThrownBy(() -> notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload))
            .isInstanceOf(NotificationException.class)
            .hasMessageContaining(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("템플릿 알림 전송 테스트_실패_빈 문자열 필드")
    void sendTemplateNotification_Fail_EmptyStringFields() {
        // Given
        Long userId = testUser.getId();
        NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("")  // 빈 문자열
            .orderId("12345")
            .build();
        // When & Then
        assertThatThrownBy(() -> notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userName is required for ORDER_COMPLETED notification");
    }

    @Test
    @DisplayName("다중 사용자 템플릿 알림 전송 테스트_성공")
    void sendTemplateNotificationToMultipleUsers_Success() {
        // Given
        User user2 = userRepository.save(createTestUser("김철수", "kim@example.com"));
        User user3 = userRepository.save(createTestUser("이영희", "lee@example.com"));
        List<Long> userIds = List.of(testUser.getId(), user2.getId(), user3.getId());
        NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        // When
        notificationSendService.sendTemplateNotificationToMultipleUsers(userIds, eventType, payload);
        // Then
        for (Long userId : userIds) {
            List<UserNotification> notifications = userNotificationRepository
                .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
            assertThat(notifications).isNotEmpty();
            UserNotification notification = notifications.get(0);
            assertThat(notification.getTemplate().getId()).isEqualTo(testTemplate.getId());
            assertThat(notification.getContextData()).isEqualTo(payload.toMap());
        }
        verify(sseService, times(3)).sendNotification(anyLong(), anyString());
    }

    @Test
    @DisplayName("다중 사용자 템플릿 알림 전송 테스트_빈 리스트")
    void sendTemplateNotificationToMultipleUsers_EmptyList() {
        // Given
        List<Long> userIds = Collections.emptyList();
        NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        // When
        notificationSendService.sendTemplateNotificationToMultipleUsers(userIds, eventType, payload);
        // Then - 예외 없이 정상 완료되어야 함
        verify(sseService, never()).sendNotification(anyLong(), anyString());
    }

    @Test
    @DisplayName("다중 사용자 템플릿 알림 전송 테스트_일부 사용자 존재하지 않음")
    void sendTemplateNotificationToMultipleUsers_PartialUserNotFound() {
        // Given
        User user2 = userRepository.save(createTestUser("김철수", "kim@example.com"));
        List<Long> userIds = List.of(testUser.getId(), 9999999999L, user2.getId());
        NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        // When & Then - 일부 사용자가 없어도 예외 발생 (RuntimeException)
        assertThatThrownBy(() -> notificationSendService.sendTemplateNotificationToMultipleUsers(userIds, eventType, payload))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("커스텀 알림 전송 테스트_성공")
    void sendCustomNotificationToSingleUser_Success() {
        // Given
        Long userId = testUser.getId();
        NotificationCategoryType categoryType = NotificationCategoryType.CUSTOM;
        String title = "시스템 점검 안내";
        String content = "시스템 점검이 예정되어 있습니다.";
        String redirectUrl = "/maintenance";
        Map<String, Object> contextData = Map.of("maintenanceTime", "2024-01-01 00:00");
        // When
        notificationSendService.sendCustomNotificationToSingleUser(userId, categoryType, title, content, redirectUrl, contextData);
        // Then
        List<UserNotification> notifications = userNotificationRepository
            .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        assertThat(notifications).isNotEmpty();
        UserNotification notification = notifications.get(0);
        assertThat(notification.getTemplate().getTitle()).isEqualTo(title);
        assertThat(notification.getTemplate().getContent()).isEqualTo(content);
        assertThat(notification.getTemplate().getRedirectUrl()).isEqualTo(redirectUrl);
        assertThat(notification.getTemplate().getTemplateType()).isEqualTo(NotificationEventType.CUSTOM);
        assertThat(notification.getContextData()).isEqualTo(contextData);
        verify(sseService, times(1)).sendNotification(eq(userId), anyString());
    }

    @Test
    @DisplayName("커스텀 알림 전송 테스트_실패_contextData가 null")
    void sendCustomNotificationToSingleUser_Fail_NullContextData() {
        // Given
        Long userId = testUser.getId();
        NotificationCategoryType categoryType = NotificationCategoryType.CUSTOM;
        String title = "시스템 점검 안내";
        String content = "시스템 점검이 예정되어 있습니다.";
        String redirectUrl = "/maintenance";
        Map<String, Object> contextData = null;
        // When & Then
        assertThatThrownBy(() -> notificationSendService.sendCustomNotificationToSingleUser(
            userId, categoryType, title, content, redirectUrl, contextData))
            .isInstanceOf(NotificationException.class)
            .hasMessageContaining(ErrorCode.NOTIFICATION_CONTEXT_DATA_IS_NULL.getMessage());
    }

    @Test
    @DisplayName("커스텀 알림 전송 테스트_성공_contextData가 빈 Map")
    void sendCustomNotificationToSingleUser_Success_EmptyContextData() {
        // Given
        Long userId = testUser.getId();
        NotificationCategoryType categoryType = NotificationCategoryType.CUSTOM;
        String title = "시스템 점검 안내";
        String content = "시스템 점검이 예정되어 있습니다.";
        String redirectUrl = "/maintenance";
        Map<String, Object> contextData = Collections.emptyMap();
        // When & Then
        notificationSendService.sendCustomNotificationToSingleUser(userId, categoryType, title, content, redirectUrl, contextData);
        // Then
        List<UserNotification> notifications = userNotificationRepository
            .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        assertThat(notifications).isNotEmpty();
        UserNotification notification = notifications.get(0);
        assertThat(notification.getTemplate().getTitle()).isEqualTo(title);
        assertThat(notification.getTemplate().getContent()).isEqualTo(content);
        assertThat(notification.getTemplate().getRedirectUrl()).isEqualTo(redirectUrl);
        assertThat(notification.getContextData()).isEqualTo(contextData);
        verify(sseService, times(1)).sendNotification(eq(userId), anyString());
    }

    @Test
    @DisplayName("다중 사용자 커스텀 알림 전송 테스트_성공")
    void sendCustomNotificationToMultipleUsers_Success() {
        // Given
        User user2 = userRepository.save(createTestUser("김철수", "kim@example.com"));
        User user3 = userRepository.save(createTestUser("이영희", "lee@example.com"));
        List<Long> userIds = List.of(testUser.getId(), user2.getId(), user3.getId());
        NotificationCategoryType categoryType = NotificationCategoryType.PROMOTION;
        String title = "프로모션 안내";
        String content = "특별 할인 이벤트가 시작되었습니다.";
        String redirectUrl = "/promotion";
        Map<String, Object> contextData = Map.of("promotionName", "신년 특가");
        // When
        notificationSendService.sendCustomNotificationToMultipleUsers(userIds, categoryType, title, content, redirectUrl, contextData);
        // Then
        for (Long userId : userIds) {
            List<UserNotification> notifications = userNotificationRepository
                .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
            assertThat(notifications).isNotEmpty();
            UserNotification notification = notifications.get(0);
            assertThat(notification.getTemplate().getTitle()).isEqualTo(title);
            assertThat(notification.getTemplate().getContent()).isEqualTo(content);
        }
        verify(sseService, times(3)).sendNotification(anyLong(), anyString());
    }

    @Test
    @DisplayName("다중 사용자 커스텀 알림 전송 테스트_빈 리스트")
    void sendCustomNotificationToMultipleUsers_EmptyList() {
        // Given
        List<Long> userIds = Collections.emptyList();
        NotificationCategoryType categoryType = NotificationCategoryType.PROMOTION;
        String title = "프로모션 안내";
        String content = "특별 할인 이벤트가 시작되었습니다.";
        String redirectUrl = "/promotion";
        Map<String, Object> contextData = Map.of("promotionName", "신년 특가");
        // When
        notificationSendService.sendCustomNotificationToMultipleUsers(userIds, categoryType, title, content, redirectUrl, contextData);
        // Then - 예외 없이 정상 완료되어야 함
        verify(sseService, never()).sendNotification(anyLong(), anyString());
    }

    @Test
    @DisplayName("다중 사용자 커스텀 알림 전송 테스트_일부 사용자 존재하지 않음")
    void sendCustomNotificationToMultipleUsers_PartialUserNotFound() {
        // Given
        User user2 = userRepository.save(createTestUser("김철수", "kim@example.com"));
        List<Long> userIds = List.of(testUser.getId(), 9999999999L, user2.getId());
        NotificationCategoryType categoryType = NotificationCategoryType.PROMOTION;
        String title = "프로모션 안내";
        String content = "특별 할인 이벤트가 시작되었습니다.";
        String redirectUrl = "/promotion";
        Map<String, Object> contextData = Map.of("promotionName", "신년 특가");
        // When & Then
        assertThatThrownBy(() -> notificationSendService.sendCustomNotificationToMultipleUsers(
            userIds, categoryType, title, content, redirectUrl, contextData))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("중복 알림 전송 테스트_같은 사용자에게 같은 템플릿으로 여러 번 전송 가능")
    void sendTemplateNotification_DuplicateNotification() {
        // Given
        Long userId = testUser.getId();
        NotificationEventType eventType = NotificationEventType.ORDER_COMPLETED;
        OrderNotificationPayload.OrderCompletedPayload payload = OrderNotificationPayload.OrderCompletedPayload.builder()
            .userName("홍길동")
            .orderId("12345")
            .build();
        // When - 같은 알림을 3번 전송
        notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload);
        notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload);
        notificationSendService.sendTemplateNotificationToSingleUser(userId, eventType, payload);
        // Then - 모두 성공적으로 저장되어야 함
        List<UserNotification> notifications = userNotificationRepository
            .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        assertThat(notifications.size()).isGreaterThanOrEqualTo(3);
        verify(sseService, times(3)).sendNotification(eq(userId), anyString());
    }

    private void setUpNotificationCategoryOnDatabase() {
        NotificationCategoryType[] categoryTypes = NotificationCategoryType.values();
        for (NotificationCategoryType categoryType : categoryTypes) {
            notificationCategoryRepository.save(NotificationCategory.builder()
                .categoryType(categoryType)
                .build());
        }
    }
    private User createTestUser() {
        return createTestUser("홍길동", "honggildong@example.com");
    }

    private User createTestUser(String name, String email) {
        return User.builder()
            .name(name)
            .email(email)
            .provider(OAuth2Provider.KAKAO)
            .providerId("123456789")
            .role(UserRole.USER)
            .build();
    }   

    private NotificationTemplate createTestNotificationTemplate() {
        return NotificationTemplate.builder()
            .category(testCategory)
            .templateType(NotificationEventType.ORDER_COMPLETED)
            .title("주문 완료")
            .content("주문이 완료되었습니다.")
            .redirectUrl("/order/{orderId}")
            .build();
    }   
}
