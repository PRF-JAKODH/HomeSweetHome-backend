package com.homesweet.homesweetback.domain.notification.entity;

import com.homesweet.homesweetback.common.BaseEntity;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 템플릿 엔티티
 * 
 * @author dogyungkim
 */
@Entity
@Table(name = "notification_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_template_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_category_id", nullable = false)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 50)
    private NotificationEventType templateType;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "content", nullable = false, length = 200)
    private String content;

    @Column(name = "redirect_url", nullable = false, length = 255)
    private String redirectUrl;

    @Builder
    public NotificationTemplate(NotificationCategory category, 
                            NotificationEventType templateType, 
                            String title, 
                            String content, 
                            String redirectUrl) {
        this.category = category;
        this.templateType = templateType;
        this.title = title;
        this.content = content;
        this.redirectUrl = redirectUrl;
    }
}
