package com.homesweet.homesweetback.domain.notification.service;

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
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;
import com.homesweet.homesweetback.domain.notification.repository.NotificationCategoryRepository;
import com.homesweet.homesweetback.domain.notification.repository.NotificationTemplateRepository;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationRepository;
import com.homesweet.homesweetback.domain.notification.service.impl.NotificationAPIService;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationAPIService 테스트")
@TestInstance(Lifecycle.PER_CLASS)
class NotificationAPIServiceTest {
    
    @Autowired
    private NotificationAPIService notificationAPIService;
    
    @Autowired
    private UserNotificationRepository userNotificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationCategoryRepository notificationCategoryRepository;
    
    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;
    
    private User testUser;
    private NotificationCategory testCategory;
    private NotificationTemplate testTemplate;

    @BeforeAll
    void setUpAll() {

        // 테스트 사용자 생성
        testUser = userRepository.save(createTestUser());
         // 테스트 카테고리 생성
        testCategory = notificationCategoryRepository.save(NotificationCategory.builder()
                .categoryType(NotificationCategoryType.ORDER)
                .build());
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
    @DisplayName("알림 조회 - 성공: 여러 개의 알림이 있을 때")
    void getAllNotifications_Success() {
        // Given: 테스트 알림 데이터 생성
        createAndSaveManyUserNotifications(testUser, testTemplate, 40); 
        
        // When: 알림 조회
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());
        
        // Then: 알림 목록이 최신순으로 반환되어야 함
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.get(0).getTitle()).isEqualTo(testTemplate.getTitle());
        assertThat(result.get(0).getContextData()).containsEntry("orderId", "39");
        assertThat(result.get(0).isRead()).isFalse();
        assertThat(result.get(0).getCategoryType()).isEqualTo(testTemplate.getCategory().getCategoryType());
        assertThat(result.get(0).getCreatedAt()).isNotNull();
        assertThat(result.get(0).getNotificationId()).isNotNull();
        
        assertThat(result.get(19).getTitle()).isEqualTo(testTemplate.getTitle());
        assertThat(result.get(19).getContextData()).containsEntry("orderId", "20");
    }

    @Test
    @DisplayName("알림 조회 - 성공: 최신순으로 반환되어야 함")
    void getAllNotifications_Success_Latest() {
        // Given: 테스트 알림 데이터 생성
        createAndSaveManyUserNotifications(testUser, testTemplate, 40);
        
        // When: 알림 조회
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());

        // Then: 알림 목록이 최신순으로 반환되어야 함
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.get(0).getTitle()).isEqualTo(testTemplate.getTitle());
        assertThat(result.get(0).getContextData()).containsEntry("orderId", "39");
        assertThat(result.get(0).isRead()).isFalse();
        assertThat(result.get(0).getCategoryType()).isEqualTo(testTemplate.getCategory().getCategoryType());
        assertThat(result.get(0).getCreatedAt()).isNotNull();
        assertThat(result.get(0).getNotificationId()).isNotNull();
    }
    
    @Test
    @DisplayName("알림 조회 - 성공: 알림이 없을 때 빈 리스트 반환")
    void getAllNotifications_EmptyList() {
        // Given: 알림 데이터가 없음
        
        // When: 알림 조회
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());
        
        // Then: 빈 리스트 반환
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("알림 조회 - 20개를 초과하는 경우 20개만 반환")
    void getAllNotifications_Max20() {
        // Given: 25개의 알림 데이터 생성
        createAndSaveManyUserNotifications(testUser, testTemplate, 25);
        
        // When: 알림 조회
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());
        
        // Then: 20개만 반환되어야 함
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(20);
    }
    
    @Test
    @DisplayName("알림 조회 - 삭제된 알림은 조회되지 않음")
    void getAllNotifications_ExcludeDeleted() {
        // Given: 읽지 않은 알림과 삭제된 알림 생성
        List<UserNotification> userNotifications = createAndSaveManyUserNotifications(testUser, testTemplate, 2);
        userNotifications.get(0).markAsDeleted();
        userNotificationRepository.save(userNotifications.get(0));
        // When: 알림 조회
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());
        
        // Then: 삭제되지 않은 알림만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getContextData()).containsEntry("orderId", "1");
    }
    
    @Test
    @DisplayName("알림 읽음 처리 - 1개 알림 읽음 처리 성공")
    void markAsRead_SingleNotification() {
        // Given: 읽지 않은 알림 1개 생성
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("orderId", "1");
        
        UserNotification notification = createTestUserNotification(testUser, testTemplate, contextData);
        
        notification = userNotificationRepository.save(notification);
        
        // When: 알림 읽음 처리
        notificationAPIService.markAsRead(testUser.getId(), Arrays.asList(notification.getId()));
        
        // Then: 알림이 읽음 처리되어야 함
        UserNotification updatedNotification = userNotificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(updatedNotification.getIsRead()).isTrue();
    }
    
    @Test
    @DisplayName("알림 읽음 처리 - 여러 개 알림 읽음 처리 성공")
    void markAsRead_MultipleNotifications() {
        // Given: 읽지 않은 알림 3개 생성
        List<UserNotification> userNotifications = createAndSaveManyUserNotifications(testUser, testTemplate, 3);
        // When: 여러 개 알림 읽음 처리
        notificationAPIService.markAsRead(testUser.getId(), Arrays.asList(
                userNotifications.get(0).getId(), 
                userNotifications.get(1).getId(), 
                userNotifications.get(2).getId()
        ));
        
        // Then: 모든 알림이 읽음 처리되어야 함
        UserNotification updatedNotification1 = userNotificationRepository.findById(userNotifications.get(0).getId()).orElseThrow();
        UserNotification updatedNotification2 = userNotificationRepository.findById(userNotifications.get(1).getId()).orElseThrow();
        UserNotification updatedNotification3 = userNotificationRepository.findById(userNotifications.get(2).getId()).orElseThrow();
        
        assertThat(updatedNotification1.getIsRead()).isTrue();
        assertThat(updatedNotification2.getIsRead()).isTrue();
        assertThat(updatedNotification3.getIsRead()).isTrue();
    }
    
    @Test
    @DisplayName("알림 읽음 처리 - 알림을 찾을 수 없을 때 예외 발생")
    void markAsRead_NotFound() {
        // Given: 존재하지 않는 알림 ID
        
        // When & Then: 알림을 찾을 수 없어서 예외 발생
        assertThatThrownBy(() -> {
            notificationAPIService.markAsRead(testUser.getId(), Arrays.asList(99999L));
        })
        .isInstanceOf(NotificationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
    
    @Test
    @DisplayName("알림 읽음 처리 - 다른 사용자의 알림에 접근할 수 없음")
    void markAsRead_AccessDenied() {
        // Given: 다른 사용자와 해당 사용자의 알림 생성
        User otherUser = User.builder()
                .email("other@example.com")
                .name("다른 사용자")
                .provider(OAuth2Provider.KAKAO)
                .providerId("987654321")
                .role(UserRole.USER)
                .build();
        otherUser = userRepository.save(otherUser);
        List<UserNotification> userNotifications = createAndSaveManyUserNotifications(otherUser, testTemplate, 1);
        
        final UserNotification finalNotification = userNotifications.get(0);
        
        // When & Then: 다른 사용자의 알림 ID로 읽음 처리 시도 → 예외 발생
        assertThatThrownBy(() -> {
            notificationAPIService.markAsRead(testUser.getId(), Arrays.asList(finalNotification.getId()));
        })
        .isInstanceOf(NotificationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
    
    @Test
    @DisplayName("알림 삭제 처리 - 1개 삭제 성공")
    void markAsDeleted_SingleNotification() {
        // Given: 삭제되지 않은 알림 1개 생성
        UserNotification notification = createTestUserNotification(testUser, testTemplate, Map.of("orderId", "1"));
        notification = userNotificationRepository.save(notification);
        // When: 알림 삭제 처리
        notificationAPIService.markAsDeleted(testUser.getId(), Arrays.asList(notification.getId()));
        
        // Then: 알림이 삭제 처리되어야 함
        UserNotification updatedNotification = userNotificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(updatedNotification.getIsDeleted()).isTrue();
    }
    
    @Test
    @DisplayName("알림 삭제 처리 - 여러 개 삭제 성공")
    void markAsDeleted_MultipleNotifications() {
        // Given: 삭제되지 않은 알림 3개 생성
        List<UserNotification> userNotifications = createAndSaveManyUserNotifications(testUser, testTemplate, 3);
        
        // When: 여러 개 알림 삭제 처리
        notificationAPIService.markAsDeleted(testUser.getId(), Arrays.asList(
                userNotifications.get(0).getId(), 
                userNotifications.get(1).getId(), 
                userNotifications.get(2).getId()
        ));
        
        // Then: 모든 알림이 삭제 처리되어야 함
        UserNotification updatedNotification1 = userNotificationRepository.findById(userNotifications.get(0).getId()).orElseThrow();
        UserNotification updatedNotification2 = userNotificationRepository.findById(userNotifications.get(1).getId()).orElseThrow();
        UserNotification updatedNotification3 = userNotificationRepository.findById(userNotifications.get(2).getId()).orElseThrow();
        
        assertThat(updatedNotification1.getIsDeleted()).isTrue();
        assertThat(updatedNotification2.getIsDeleted()).isTrue();
        assertThat(updatedNotification3.getIsDeleted()).isTrue();
    }
    
    @Test
    @DisplayName("알림 삭제 처리 - 알림을 찾을 수 없을 때 예외 발생")
    void markAsDeleted_NotFound() {
        // Given: 존재하지 않는 알림 ID
        
        // When & Then: 알림을 찾을 수 없어서 예외 발생
        assertThatThrownBy(() -> {
            notificationAPIService.markAsDeleted(testUser.getId(), Arrays.asList(99999L));
        })
        .isInstanceOf(NotificationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
    
    @Test
    @DisplayName("알림 삭제 처리 - 다른 사용자의 알림에 접근할 수 없음")
    void markAsDeleted_AccessDenied() {
        // Given: 다른 사용자와 해당 사용자의 알림 생성
        User otherUser = User.builder()
                .email("other@example.com")
                .name("다른 사용자")
                .provider(OAuth2Provider.KAKAO)
                .providerId("987654321")
                .role(UserRole.USER)
                .build();
        otherUser = userRepository.save(otherUser);
        
        List<UserNotification> userNotifications = createAndSaveManyUserNotifications(otherUser, testTemplate, 1);
        final UserNotification finalDeleteNotification = userNotifications.get(0);
        
        // When & Then: 다른 사용자의 알림 ID로 삭제 처리 시도 → 예외 발생
        assertThatThrownBy(() -> {
            notificationAPIService.markAsDeleted(testUser.getId(), Arrays.asList(finalDeleteNotification.getId()));
        })
        .isInstanceOf(NotificationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    private List<UserNotification> createAndSaveManyUserNotifications(User user, NotificationTemplate template, int count) {
        List<UserNotification> userNotifications = new ArrayList<>(count);
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("userName", user.getName());
        for (int i = 0; i < count; i++) {
            contextData.put("orderId", String.valueOf(i));
            UserNotification userNotification = userNotificationRepository.save(createTestUserNotification(user, template, contextData));
            userNotifications.add(userNotification);
        }
        return userNotifications;
    }

    private UserNotification createTestUserNotification(User user, NotificationTemplate template, Map<String, Object> contextData) {
        return UserNotification.builder()
            .user(user)
            .template(template)
            .contextData(contextData)
            .isRead(false)
            .isDeleted(false)
            .build();
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
            .category(testCategory)  // 저장된 카테고리 사용
            .templateType(NotificationEventType.ORDER_COMPLETED)
            .title("주문 완료")
            .content("주문이 완료되었습니다.")
            .redirectUrl("/order/{orderId}")
            .build();
    }   
}
