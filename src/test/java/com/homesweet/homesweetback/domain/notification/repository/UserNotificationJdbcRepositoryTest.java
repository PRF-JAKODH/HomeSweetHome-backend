package com.homesweet.homesweetback.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.repository.impl.H2UserNotificationJdbcRepository;
import com.homesweet.homesweetback.domain.product.category.repository.impl.ProductCategoryRepositoryImpl;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        QueryDslConfig.class,
        ProductCategoryRepositoryImpl.class,
        ProductCategoryMapper.class,
        H2UserNotificationJdbcRepository.class,
        ObjectMapper.class
})
public class UserNotificationJdbcRepositoryTest {

    @Autowired
    private UserNotificationJdbcRepository userNotificationJdbcRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private NotificationCategoryRepository notificationCategoryRepository;

    private User testUser;
    private NotificationCategory testCategory;
    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(createTestUser());

        testCategory = NotificationCategory.builder()
                .categoryType(NotificationCategoryType.ORDER)
                .build();
        notificationCategoryRepository.save(testCategory);

        testTemplate = createTestNotificationTemplate(testCategory);
        notificationTemplateRepository.save(testTemplate);
    }

    @Test
    @DisplayName("Bulk Insert 테스트 - H2")
    void saveAll_Success() {
        // Given
        int count = 100;
        List<UserNotification> notifications = createTestUserNotificationList(testUser, testTemplate, count);

        // When
        userNotificationJdbcRepository.saveAll(notifications);

        // Then
        List<UserNotification> savedNotifications = userNotificationRepository.findAll();
        assertThat(savedNotifications).hasSize(count);

        // ID가 제대로 할당되었는지 확인
        for (UserNotification notification : notifications) {
            assertThat(notification.getId()).isNotNull();
        }

        // 데이터 검증 (첫번째 요소)
        UserNotification first = savedNotifications.get(0);
        assertThat(first.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(first.getTemplate().getId()).isEqualTo(testTemplate.getId());
        assertThat(first.getContextData().get("orderId").toString())
                .isEqualTo(notifications.get(0).getContextData().get("orderId").toString());
    }

    private User createTestUser() {
        return User.builder()
                .email("test@example.com")
                .name("테스트 사용자")
                .address("서울시 강남구 역삼동")
                .phoneNumber("01012345678")
                .birthDate(LocalDate.of(1990, 1, 1))
                .profileImageUrl("https://example.com/profile.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .role(UserRole.USER)
                .build();
    }

    private NotificationTemplate createTestNotificationTemplate(NotificationCategory category) {
        return NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.ORDER_COMPLETED)
                .title("주문 완료")
                .content("주문이 완료되었습니다.")
                .redirectUrl("/order/{orderId}")
                .build();
    }

    private UserNotification createTestUserNotification(User user, NotificationTemplate template) {
        Long orderId = (long) (Math.random() * 1000000000);
        return UserNotification.builder()
                .user(user)
                .template(template)
                .contextData(Map.of("orderId", orderId))
                .isRead(false)
                .isDeleted(false)
                .build();
    }

    private List<UserNotification> createTestUserNotificationList(User user, NotificationTemplate template, int count) {
        List<UserNotification> userNotificationList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UserNotification userNotification = createTestUserNotification(user, template);
            userNotificationList.add(userNotification);
        }
        return userNotificationList;
    }
}
