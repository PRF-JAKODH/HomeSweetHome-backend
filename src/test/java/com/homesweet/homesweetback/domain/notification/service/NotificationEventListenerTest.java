package com.homesweet.homesweetback.domain.notification.service;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.domain.event.CustomNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;
import com.homesweet.homesweetback.domain.notification.domain.notification.OrderNotification;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationEventListener 테스트")
@TestInstance(Lifecycle.PER_CLASS)
public class NotificationEventListenerTest {

        @Autowired
        private ApplicationEventPublisher eventPublisher;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private NotificationTemplateRepository notificationTemplateRepository;

        @Autowired
        private UserNotificationRepository userNotificationRepository;

        private User testUser;
        private User testUser2;
        private NotificationTemplate testTemplate;

        @BeforeAll
        void setUpAll() throws Exception {
                testUser = userRepository.save(User.builder()
                                .name("홍길동")
                                .email("honggildong@example.com")
                                .provider(OAuth2Provider.GOOGLE)
                                .providerId("123456789")
                                .role(UserRole.USER)
                                .build());

                testUser2 = userRepository.save(User.builder()
                                .name("김철수")
                                .email("kimchulsu@example.com")
                                .provider(OAuth2Provider.GOOGLE)
                                .providerId("123456789")
                                .role(UserRole.USER)
                                .build());

                testTemplate = notificationTemplateRepository
                                .findByTemplateType(NotificationTemplateType.ORDER_COMPLETED)
                                .orElseThrow(() -> new RuntimeException("ORDER_COMPLETED 템플릿을 찾을 수 없습니다."));
        }

        @AfterEach
        void tearDown() {
                userNotificationRepository.deleteAllInBatch();
        }

        @AfterAll
        void tearDownAll() {
                if (testUser != null)
                        userRepository.delete(testUser);
                if (testUser2 != null)
                        userRepository.delete(testUser2);
        }

        @Test
        @DisplayName("템플릿 알림 이벤트 발행 테스트_성공")
        void testSendTemplateNotificationEvent() {
                // Given
                OrderNotification.OrderCompleted notification = OrderNotification.OrderCompleted.builder()
                                .userName("홍길동")
                                .orderId(12345L)
                                .build();

                // When
                eventPublisher.publishEvent(new TemplateNotificationEvent(List.of(testUser.getId()), notification));

                // Then
                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(testUser.getId());
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isEqualTo(1);
                        assertThat(userNotifications.get(0).getTemplate().getTemplateType())
                                        .isEqualTo(testTemplate.getTemplateType());
                        assertThat(userNotifications.get(0).getContextData())
                                        .isEqualTo(Map.of("userName", "홍길동", "orderId", 12345));
                        assertThat(userNotifications.get(0).getIsRead()).isFalse();
                });
        }

        @Test
        @DisplayName("템플릿 알림 이벤트 발행 테스트_다수 사용자_성공")
        void testSendTemplateNotificationEvent_MultipleUsers() {
                // Given
                OrderNotification.OrderCompleted notification = OrderNotification.OrderCompleted.builder()
                                .userName("홍길동")
                                .orderId(12345L)
                                .build();

                eventPublisher.publishEvent(
                                new TemplateNotificationEvent(List.of(testUser.getId(), testUser2.getId()),
                                                notification));

                // Then
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(testUser.getId());
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isEqualTo(1);
                        assertThat(userNotifications.get(0).getTemplate().getTemplateType())
                                        .isEqualTo(testTemplate.getTemplateType());
                        assertThat(userNotifications.get(0).getContextData())
                                        .isEqualTo(Map.of("userName", "홍길동", "orderId", 12345));
                        assertThat(userNotifications.get(0).getIsRead()).isFalse();
                });
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(testUser2.getId());
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isEqualTo(1);
                        assertThat(userNotifications.get(0).getTemplate().getTemplateType())
                                        .isEqualTo(testTemplate.getTemplateType());
                        assertThat(userNotifications.get(0).getContextData())
                                        .isEqualTo(Map.of("userName", "홍길동", "orderId", 12345));
                        assertThat(userNotifications.get(0).getIsRead()).isFalse();
                });
        }

        @Test
        @DisplayName("템플릿 알림 이벤트 발행 테스트_다수 사용자_실패_사용자가 없을 때")
        void testSendTemplateNotificationEvent_MultipleUsers_Failure_UserNotFound() {
                // Given
                OrderNotification.OrderCompleted notification = OrderNotification.OrderCompleted.builder()
                                .userName("홍길동")
                                .orderId(12345L)
                                .build();
                // 존재하지 않는 사용자와 존재하는 사용자 혼합
                List<Long> userIds = List.of(10123001L, testUser.getId());

                // When
                // 사용자가 없어도 예외를 throw하지 않고 로그만 남기고 계속 진행
                eventPublisher.publishEvent(new TemplateNotificationEvent(userIds, notification));

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(10123001L);
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isEqualTo(0);
                });

                // Then
                // 존재하는 사용자에게는 알림이 전송되어야 함
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(testUser.getId());
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isGreaterThanOrEqualTo(1);
                        assertThat(userNotifications.get(0).getTemplate().getTemplateType())
                                        .isEqualTo(testTemplate.getTemplateType());
                });
        }

        @Test
        @DisplayName("커스텀 알림 이벤트 발행 테스트_성공")
        void testSendCustomNotificationEvent() {
                // Given
                CustomNotification notification = CustomNotification.builder()
                                .title("커스텀 알림 제목")
                                .content("커스텀 알림 내용")
                                .redirectUrl("app://custom")
                                .contextData(Map.of("key1", "value1", "key2", 123))
                                .build();

                // When
                eventPublisher.publishEvent(new CustomNotificationEvent(List.of(testUser.getId()), notification));

                // Then
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(testUser.getId());
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isEqualTo(1);
                        assertThat(userNotifications.get(0).getTemplate().getTemplateType())
                                        .isEqualTo(NotificationTemplateType.CUSTOM);
                        assertThat(userNotifications.get(0).getContextData()).isEqualTo(notification.toMap());
                        assertThat(userNotifications.get(0).getIsRead()).isFalse();
                });
        }

        @Test
        @DisplayName("커스텀 알림 이벤트 발행 테스트_다수 사용자_성공")
        void testSendCustomNotificationEvent_MultipleUsers() {
                // Given
                CustomNotification notification = CustomNotification.builder()
                                .title("커스텀 알림 제목")
                                .content("커스텀 알림 내용")
                                .redirectUrl("app://custom")
                                .contextData(Map.of("key1", "value1", "key2", 123))
                                .build();
                eventPublisher
                                .publishEvent(new CustomNotificationEvent(List.of(testUser.getId(), testUser2.getId()),
                                                notification));

                // Then
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(testUser.getId());
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isEqualTo(1);
                        assertThat(userNotifications.get(0).getTemplate().getTemplateType())
                                        .isEqualTo(NotificationTemplateType.CUSTOM);
                        assertThat(userNotifications.get(0).getContextData()).isEqualTo(notification.toMap());
                        assertThat(userNotifications.get(0).getIsRead()).isFalse();
                });

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(testUser2.getId());
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isEqualTo(1);
                        assertThat(userNotifications.get(0).getTemplate().getTemplateType())
                                        .isEqualTo(NotificationTemplateType.CUSTOM);
                        assertThat(userNotifications.get(0).getContextData()).isEqualTo(notification.toMap());
                        assertThat(userNotifications.get(0).getIsRead()).isFalse();
                });
        }

        @Test
        @DisplayName("커스텀 알림 이벤트 발행 테스트_다수 사용자_실패_사용자가 없을 때")
        void testSendCustomNotificationEvent_MultipleUsers_Failure_UserNotFound() {
                // Given
                CustomNotification notification = CustomNotification.builder()
                                .title("커스텀 알림 제목")
                                .content("커스텀 알림 내용")
                                .redirectUrl("app://custom")
                                .contextData(Map.of("key1", "value1", "key2", 123))
                                .build();
                List<Long> userIds = List.of(testUser.getId());
                eventPublisher.publishEvent(new CustomNotificationEvent(userIds, notification));

                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(1123120000L);
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isEqualTo(0);
                });

                // Then
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<UserNotification> userNotifications = getUserNotifications(testUser.getId());
                        assertThat(userNotifications).isNotNull();
                        assertThat(userNotifications.size()).isGreaterThanOrEqualTo(1);
                        assertThat(userNotifications.get(0).getTemplate().getTemplateType())
                                        .isEqualTo(NotificationTemplateType.CUSTOM);
                        assertThat(userNotifications.get(0).getContextData()).isEqualTo(notification.toMap());
                        assertThat(userNotifications.get(0).getIsRead()).isFalse();
                });
        }

        @Test
        @DisplayName("템플릿 알림 이벤트 발행 테스트_실패_사용자 리스트가 비어있을 때")
        void testSendTemplateNotificationEvent_Failure_EmptyUserList() {
                // Given
                OrderNotification.OrderCompleted notification = OrderNotification.OrderCompleted.builder()
                                .userName("홍길동")
                                .orderId(12345L)
                                .build();

                // When & Then
                assertThatThrownBy(() -> eventPublisher
                                .publishEvent(new TemplateNotificationEvent(List.of(), notification)))
                                .isInstanceOf(RuntimeException.class);

        }

        @Test
        @DisplayName("커스텀 알림 이벤트 발행 테스트_성공_사용자 리스트가 비어있을 때")
        void testSendCustomNotificationEvent_EmptyUserList() {
                // Given
                CustomNotification notification = CustomNotification.builder()
                                .title("커스텀 알림 제목")
                                .content("커스텀 알림 내용")
                                .redirectUrl("app://custom")
                                .contextData(Map.of("key1", "value1", "key2", 123))
                                .build();

                // When
                assertThatThrownBy(
                                () -> eventPublisher.publishEvent(new CustomNotificationEvent(List.of(), notification)))
                                .isInstanceOf(NotificationException.class);
        }

        private List<UserNotification> getUserNotifications(Long userId) {
                return userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        }
}
