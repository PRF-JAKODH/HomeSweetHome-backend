package com.homesweet.homesweetback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;
import com.homesweet.homesweetback.domain.notification.service.impl.NotificationAPIService;

/**
 * NotificationAPIService 엣지 케이스 테스트
 * 
 * 빈 리스트, 중복 ID, 경계값, 동시성 등 다양한 엣지 케이스를 테스트합니다.
 * 
 * @author dogyungkim
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationAPIService 엣지 케이스 테스트")
@TestInstance(Lifecycle.PER_CLASS)
class NotificationAPIServiceEdgeCaseTest {

    @Autowired
    private NotificationAPIService notificationAPIService;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    private User testUser;
    private NotificationTemplate testTemplate;

    @BeforeAll
    void setUpAll() {
        testUser = userRepository.save(createTestUser());
        testTemplate = notificationTemplateRepository.findByTemplateType(NotificationTemplateType.ORDER_COMPLETED)
                .orElseThrow(() -> new RuntimeException("ORDER_COMPLETED 템플릿을 찾을 수 없습니다."));
    }

    @AfterEach
    void tearDown() {
        userNotificationRepository.deleteAll();
    }

    // ========== 빈 리스트 처리 테스트 ==========

    @Test
    @DisplayName("알림 읽음 처리 테스트_실패_Null 알림 ID 리스트")
    void markAsRead_Failure_NullNotificationIds() {
        // When & Then
        assertThatThrownBy(() -> {
            notificationAPIService.markAsRead(testUser.getId(), null);
        })
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("알림을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("알림 읽음 처리 테스트_실패_빈 알림 ID 리스트")
    void markAsRead_Failure_EmptyNotificationIds() {
        // When & Then
        assertThatThrownBy(() -> {
            notificationAPIService.markAsRead(testUser.getId(), new ArrayList<>());
        })
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("알림을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("알림 삭제 처리 테스트_실패_Null 알림 ID 리스트")
    void markAsDeleted_Failure_NullNotificationIds() {
        // When & Then
        assertThatThrownBy(() -> {
            notificationAPIService.markAsDeleted(testUser.getId(), null);
        })
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("알림을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("알림 삭제 처리 테스트_실패_빈 알림 ID 리스트")
    void markAsDeleted_Failure_EmptyNotificationIds() {
        // When & Then
        assertThatThrownBy(() -> {
            notificationAPIService.markAsDeleted(testUser.getId(), new ArrayList<>());
        })
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("알림을 찾을 수 없습니다");
    }

    // ========== 중복 ID 처리 테스트 ==========

    @Test
    @DisplayName("알림 읽음 처리 테스트_성공_중복 ID 제거")
    void markAsRead_Success_DuplicateIds() {
        // Given
        List<UserNotification> notifications = createAndSaveManyUserNotifications(testUser, testTemplate, 3);
        Long duplicateId = notifications.get(0).getId();

        // When - 중복된 ID로 읽음 처리
        notificationAPIService.markAsRead(testUser.getId(),
                Arrays.asList(duplicateId, duplicateId, duplicateId));

        // Then - 중복이 제거되어 한 번만 처리되어야 함
        UserNotification updatedNotification = userNotificationRepository.findById(duplicateId).orElseThrow();
        assertThat(updatedNotification.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("알림 삭제 처리 테스트_성공_중복 ID 제거")
    void markAsDeleted_Success_DuplicateIds() {
        // Given
        List<UserNotification> notifications = createAndSaveManyUserNotifications(testUser, testTemplate, 3);
        Long duplicateId = notifications.get(0).getId();

        // When - 중복된 ID로 삭제 처리
        notificationAPIService.markAsDeleted(testUser.getId(),
                Arrays.asList(duplicateId, duplicateId, duplicateId));

        // Then - 중복이 제거되어 한 번만 처리되어야 함
        UserNotification updatedNotification = userNotificationRepository.findById(duplicateId).orElseThrow();
        assertThat(updatedNotification.getIsDeleted()).isTrue();
        assertThat(updatedNotification.getIsRead()).isTrue();
    }

    // ========== 경계값 테스트 ==========

    @Test
    @DisplayName("알림 조회 테스트_성공_정확히 20개")
    void getAllNotifications_Success_Exactly20Notifications() {
        // Given
        createAndSaveManyUserNotifications(testUser, testTemplate, 20);

        // When
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());

        // Then
        assertThat(result).hasSize(20);
    }

    @Test
    @DisplayName("알림 조회 테스트_성공_19개")
    void getAllNotifications_Success_19Notifications() {
        // Given
        createAndSaveManyUserNotifications(testUser, testTemplate, 19);

        // When
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());

        // Then
        assertThat(result).hasSize(19);
    }

    @Test
    @DisplayName("알림 조회 테스트_성공_21개_20개만_반환")
    void getAllNotifications_Success_21Notifications_Returns20() {
        // Given
        createAndSaveManyUserNotifications(testUser, testTemplate, 21);

        // When
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());

        // Then
        assertThat(result).hasSize(20);
    }

    @Test
    @DisplayName("알림 조회 테스트_성공_100개_20개만_반환")
    void getAllNotifications_Success_100Notifications_Returns20() {
        // Given
        createAndSaveManyUserNotifications(testUser, testTemplate, 100);

        // When
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());

        // Then
        assertThat(result).hasSize(20);
    }

    // ========== 이미 처리된 알림 테스트 ==========

    @Test
    @DisplayName("알림 읽음 처리 테스트_성공_이미_읽은_알림")
    void markAsRead_Success_AlreadyRead() {
        // Given
        UserNotification notification = createTestUserNotification(testUser, testTemplate, Map.of("orderId", "1"));
        notification.markAsRead();
        notification = userNotificationRepository.save(notification);

        // When - 이미 읽은 알림을 다시 읽음 처리
        Long notificationId = notification.getId();
        assertThatCode(() -> {
            notificationAPIService.markAsRead(testUser.getId(), Arrays.asList(notificationId));
        }).doesNotThrowAnyException();

        // Then - 여전히 읽음 상태여야 함
        UserNotification updatedNotification = userNotificationRepository.findById(notificationId).orElseThrow();
        assertThat(updatedNotification.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("알림 삭제 처리 테스트_실패_이미_삭제된_알림")
    void markAsDeleted_Failure_AlreadyDeleted() {
        // Given
        UserNotification notification = createTestUserNotification(testUser, testTemplate, Map.of("orderId", "1"));
        notification.markAsDeleted();
        notification = userNotificationRepository.save(notification);

        // When & Then - 이미 삭제된 알림은 조회되지 않아 예외 발생
        Long notificationId = notification.getId();
        assertThatThrownBy(() -> {
            notificationAPIService.markAsDeleted(testUser.getId(), Arrays.asList(notificationId));
        })
                .isInstanceOf(NotificationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    // ========== 동시성 테스트 ==========

    @Test
    @DisplayName("알림 읽음 처리 테스트_성공_동시_요청")
    void markAsRead_Success_ConcurrentRequests() throws InterruptedException {
        // Given
        List<UserNotification> notifications = createAndSaveManyUserNotifications(testUser, testTemplate, 10);
        List<Long> notificationIds = notifications.stream()
                .map(UserNotification::getId)
                .toList();

        // When - 동시에 여러 스레드에서 읽음 처리
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    notificationAPIService.markAsRead(testUser.getId(), notificationIds);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 모든 알림이 읽음 처리되어야 함
        List<UserNotification> updatedNotifications = userNotificationRepository.findAllById(notificationIds);
        assertThat(updatedNotifications).allMatch(UserNotification::getIsRead);
    }

    @Test
    @DisplayName("알림 삭제 처리 테스트_성공_동시_요청")
    void markAsDeleted_Success_ConcurrentRequests() throws InterruptedException {
        // Given
        List<UserNotification> notifications = createAndSaveManyUserNotifications(testUser, testTemplate, 10);
        List<Long> notificationIds = notifications.stream()
                .map(UserNotification::getId)
                .toList();

        // When - 동시에 여러 스레드에서 삭제 처리
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    notificationAPIService.markAsDeleted(testUser.getId(), notificationIds);
                } catch (Exception e) {
                    // 일부 스레드는 이미 삭제된 알림을 찾지 못할 수 있음
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 모든 알림이 삭제 처리되어야 함
        List<UserNotification> updatedNotifications = userNotificationRepository.findAllById(notificationIds);
        assertThat(updatedNotifications).allMatch(UserNotification::getIsDeleted);
        assertThat(updatedNotifications).allMatch(UserNotification::getIsRead);
    }

    // ========== 대량 ID 처리 테스트 ==========

    @Test
    @DisplayName("알림 읽음 처리 테스트_성공_대량_ID_50개")
    void markAsRead_Success_LargeNumberOfIds_50() {
        // Given
        List<UserNotification> notifications = createAndSaveManyUserNotifications(testUser, testTemplate, 50);
        List<Long> notificationIds = notifications.stream()
                .map(UserNotification::getId)
                .toList();

        // When
        notificationAPIService.markAsRead(testUser.getId(), notificationIds);

        // Then
        List<UserNotification> updatedNotifications = userNotificationRepository.findAllById(notificationIds);
        assertThat(updatedNotifications).allMatch(UserNotification::getIsRead);
        assertThat(updatedNotifications).hasSize(50);
    }

    @Test
    @DisplayName("알림 삭제 처리 테스트_성공_대량_ID_50개")
    void markAsDeleted_Success_LargeNumberOfIds_50() {
        // Given
        List<UserNotification> notifications = createAndSaveManyUserNotifications(testUser, testTemplate, 50);
        List<Long> notificationIds = notifications.stream()
                .map(UserNotification::getId)
                .toList();

        // When
        notificationAPIService.markAsDeleted(testUser.getId(), notificationIds);

        // Then
        List<UserNotification> updatedNotifications = userNotificationRepository.findAllById(notificationIds);
        assertThat(updatedNotifications).allMatch(UserNotification::getIsDeleted);
        assertThat(updatedNotifications).allMatch(UserNotification::getIsRead);
        assertThat(updatedNotifications).hasSize(50);
    }

    // ========== 혼합 시나리오 테스트 ==========

    @Test
    @DisplayName("알림 읽음 처리 테스트_성공_읽은_알림과_안_읽은_알림_혼합")
    void markAsRead_Success_MixedReadAndUnread() {
        // Given
        List<UserNotification> notifications = createAndSaveManyUserNotifications(testUser, testTemplate, 5);
        notifications.get(0).markAsRead();
        notifications.get(2).markAsRead();
        userNotificationRepository.saveAll(notifications);

        List<Long> notificationIds = notifications.stream()
                .map(UserNotification::getId)
                .toList();

        // When
        notificationAPIService.markAsRead(testUser.getId(), notificationIds);

        // Then - 모든 알림이 읽음 처리되어야 함
        List<UserNotification> updatedNotifications = userNotificationRepository.findAllById(notificationIds);
        assertThat(updatedNotifications).allMatch(UserNotification::getIsRead);
    }

    // Helper methods

    private List<UserNotification> createAndSaveManyUserNotifications(User user, NotificationTemplate template,
            int count) {
        List<UserNotification> userNotifications = new ArrayList<>(count);
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("userName", user.getName());
        for (int i = 0; i < count; i++) {
            contextData.put("orderId", String.valueOf(i));
            UserNotification userNotification = userNotificationRepository.save(
                    createTestUserNotification(user, template, contextData));
            userNotifications.add(userNotification);
        }
        return userNotifications;
    }

    private UserNotification createTestUserNotification(User user, NotificationTemplate template,
            Map<String, Object> contextData) {
        return UserNotification.builder()
                .user(user)
                .template(template)
                .contextData(contextData)
                .isRead(false)
                .isDeleted(false)
                .build();
    }

    private User createTestUser() {
        return User.builder()
                .name("홍길동")
                .email("honggildong@example.com")
                .provider(OAuth2Provider.KAKAO)
                .providerId("123456789")
                .role(UserRole.USER)
                .build();
    }
}
