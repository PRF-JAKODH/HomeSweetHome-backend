package com.homesweet.homesweetback.domain.notification.repository;

import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import com.homesweet.homesweetback.domain.product.product.repository.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.product.category.repository.impl.ProductCategoryRepositoryImpl;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    QueryDslConfig.class,
    ProductCategoryRepositoryImpl.class,
    ProductCategoryMapper.class,
    ProductMapper.class,
})
public class NotificationTemplateRepositoryTest {
    
    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @MockitoBean
    private CacheCategory cacheCategory;

    @Test
    @DisplayName("알림 템플릿 타입 조회")
    public void get_all_notification_templates() {
        List<NotificationTemplate> notificationTemplates = notificationTemplateRepository.findAll();

        assertThat(notificationTemplates).isNotEmpty();
    }

    @Test
    @DisplayName("특정 알림 타입으로 템플릿 조회_성공")
    public void get_notification_template_by_type_success() {
        NotificationTemplateType notificationEventType = NotificationTemplateType.NEW_COMMENT_LIKE;
        Optional<NotificationTemplate> notificationTemplate = notificationTemplateRepository.findByTemplateType(notificationEventType);

        assertThat(notificationTemplate).isPresent();
        assertThat(notificationTemplate.get().getTemplateType()).isEqualTo(notificationEventType);
        assertThat(notificationTemplate.get().getTitle()).isEqualTo("새 댓글 좋아요");
        assertThat(notificationTemplate.get().getContent()).isEqualTo("{userName}님이 댓글에 좋아요를 눌렀습니다.");
        assertThat(notificationTemplate.get().getRedirectUrl()).isEqualTo("/community/posts/{postId}");
    }
}
