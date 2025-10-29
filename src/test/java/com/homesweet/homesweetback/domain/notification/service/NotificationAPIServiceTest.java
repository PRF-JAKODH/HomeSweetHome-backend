package com.homesweet.homesweetback.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
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

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationAPIService 테스트")
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
    
    @Autowired
    private DataSource dataSource;
    
    private User testUser;
    private NotificationCategory testCategory;
    private NotificationTemplate testTemplate;
    
    @BeforeEach
    void setUp() throws Exception {
        // DB 초기화
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new PathResource("src/main/resources/db/data.sql"));
        }
        
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .name("테스트 사용자")
                .provider(OAuth2Provider.KAKAO)
                .providerId("123456789")
                .role(UserRole.USER)
                .build();
        testUser = userRepository.save(testUser);
        
        // 테스트 카테고리 생성
        testCategory = NotificationCategory.builder()
                .categoryType(NotificationCategoryType.ORDER)
                .build();
        testCategory = notificationCategoryRepository.save(testCategory);
        
        // 테스트 템플릿 생성
        testTemplate = NotificationTemplate.builder()
                .category(testCategory)
                .templateType(NotificationEventType.ORDER_COMPLETED)
                .title("주문 완료")
                .content("{userName}님의 주문이 완료되었습니다.")
                .redirectUrl("app://order/{orderId}")
                .build();
        testTemplate = notificationTemplateRepository.save(testTemplate);
        
        // 기존 알림 데이터 정리
        userNotificationRepository.deleteAll();
    }
    
    @Test
    @DisplayName("알림 조회 - 성공: 여러 개의 알림이 있을 때")
    void getAllNotifications_Success() {
        // Given: 테스트 알림 데이터 생성
        Map<String, Object> contextData1 = new HashMap<>();
        contextData1.put("orderId", "12345");
        contextData1.put("userName", "홍길동");
        
        Map<String, Object> contextData2 = new HashMap<>();
        contextData2.put("orderId", "12346");
        contextData2.put("userName", "홍길동");
        
        UserNotification notification1 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData1)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        UserNotification notification2 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData2)
                .isRead(true)
                .isDeleted(false)
                .build();
        
        // 시간 차이를 두기 위해 잠시 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        userNotificationRepository.save(notification1);
        userNotificationRepository.save(notification2);
        
        // When: 알림 조회
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());
        
        // Then: 알림 목록이 최신순으로 반환되어야 함
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getTitle()).isEqualTo("주문 완료");
        assertThat(result.get(0).getContextData()).containsEntry("orderId", "12346");
        assertThat(result.get(0).isRead()).isTrue();
        assertThat(result.get(1).getContextData()).containsEntry("orderId", "12345");
        assertThat(result.get(1).isRead()).isFalse();
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
        for (int i = 0; i < 25; i++) {
            Map<String, Object> contextData = new HashMap<>();
            contextData.put("orderId", String.valueOf(i));
            contextData.put("userName", "홍길동");
            
            UserNotification notification = UserNotification.builder()
                    .user(testUser)
                    .template(testTemplate)
                    .contextData(contextData)
                    .isRead(false)
                    .isDeleted(false)
                    .build();
            
            userNotificationRepository.save(notification);
            
            // 시간 차이를 두기 위해 잠시 대기
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
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
        Map<String, Object> contextData1 = new HashMap<>();
        contextData1.put("orderId", "12345");
        
        Map<String, Object> contextData2 = new HashMap<>();
        contextData2.put("orderId", "12346");
        
        UserNotification activeNotification = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData1)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        UserNotification deletedNotification = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData2)
                .isRead(false)
                .isDeleted(true)
                .build();
        
        userNotificationRepository.save(activeNotification);
        userNotificationRepository.save(deletedNotification);
        
        // When: 알림 조회
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());
        
        // Then: 삭제되지 않은 알림만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getContextData()).containsEntry("orderId", "12345");
    }
    
    @Test
    @DisplayName("알림 읽음 처리 - 1개 알림 읽음 처리 성공")
    void markAsRead_SingleNotification() {
        // Given: 읽지 않은 알림 1개 생성
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("orderId", "12345");
        
        UserNotification notification = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData)
                .isRead(false)
                .isDeleted(false)
                .build();
        
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
        Map<String, Object> contextData1 = new HashMap<>();
        contextData1.put("orderId", "12345");
        
        Map<String, Object> contextData2 = new HashMap<>();
        contextData2.put("orderId", "12346");
        
        Map<String, Object> contextData3 = new HashMap<>();
        contextData3.put("orderId", "12347");
        
        UserNotification notification1 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData1)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        UserNotification notification2 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData2)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        UserNotification notification3 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData3)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        notification1 = userNotificationRepository.save(notification1);
        // 시간 차이를 두기 위해 잠시 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        notification2 = userNotificationRepository.save(notification2);
        // 시간 차이를 두기 위해 잠시 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        notification3 = userNotificationRepository.save(notification3);
        
        // When: 여러 개 알림 읽음 처리
        notificationAPIService.markAsRead(testUser.getId(), Arrays.asList(
                notification1.getId(), 
                notification2.getId(), 
                notification3.getId()
        ));
        
        // Then: 모든 알림이 읽음 처리되어야 함
        UserNotification updatedNotification1 = userNotificationRepository.findById(notification1.getId()).orElseThrow();
        UserNotification updatedNotification2 = userNotificationRepository.findById(notification2.getId()).orElseThrow();
        UserNotification updatedNotification3 = userNotificationRepository.findById(notification3.getId()).orElseThrow();
        
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
        
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("orderId", "12345");
        
        UserNotification otherUserNotification = UserNotification.builder()
                .user(otherUser)
                .template(testTemplate)
                .contextData(contextData)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        otherUserNotification = userNotificationRepository.save(otherUserNotification);
        final UserNotification finalNotification = otherUserNotification;
        
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
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("orderId", "12345");
        
        UserNotification notification = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData)
                .isRead(false)
                .isDeleted(false)
                .build();
        
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
        Map<String, Object> contextData1 = new HashMap<>();
        contextData1.put("orderId", "12345");
        
        Map<String, Object> contextData2 = new HashMap<>();
        contextData2.put("orderId", "12346");
        
        Map<String, Object> contextData3 = new HashMap<>();
        contextData3.put("orderId", "12347");
        
        UserNotification notification1 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData1)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        UserNotification notification2 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData2)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        UserNotification notification3 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(contextData3)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        notification1 = userNotificationRepository.save(notification1);
        // 시간 차이를 두기 위해 잠시 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        notification2 = userNotificationRepository.save(notification2);
        // 시간 차이를 두기 위해 잠시 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        notification3 = userNotificationRepository.save(notification3);
        
        // When: 여러 개 알림 삭제 처리
        notificationAPIService.markAsDeleted(testUser.getId(), Arrays.asList(
                notification1.getId(), 
                notification2.getId(), 
                notification3.getId()
        ));
        
        // Then: 모든 알림이 삭제 처리되어야 함
        UserNotification updatedNotification1 = userNotificationRepository.findById(notification1.getId()).orElseThrow();
        UserNotification updatedNotification2 = userNotificationRepository.findById(notification2.getId()).orElseThrow();
        UserNotification updatedNotification3 = userNotificationRepository.findById(notification3.getId()).orElseThrow();
        
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
        
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("orderId", "12345");
        
        UserNotification otherUserNotification = UserNotification.builder()
                .user(otherUser)
                .template(testTemplate)
                .contextData(contextData)
                .isRead(false)
                .isDeleted(false)
                .build();
        
        otherUserNotification = userNotificationRepository.save(otherUserNotification);
        final UserNotification finalDeleteNotification = otherUserNotification;
        
        // When & Then: 다른 사용자의 알림 ID로 삭제 처리 시도 → 예외 발생
        assertThatThrownBy(() -> {
            notificationAPIService.markAsDeleted(testUser.getId(), Arrays.asList(finalDeleteNotification.getId()));
        })
        .isInstanceOf(NotificationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
