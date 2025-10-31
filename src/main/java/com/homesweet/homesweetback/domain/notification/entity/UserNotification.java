package com.homesweet.homesweetback.domain.notification.entity;

import com.homesweet.homesweetback.common.BaseEntity;
import com.homesweet.homesweetback.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 사용자 알림 엔티티
 * 
 * @author dogyungkim
 */
@Entity
@Table(name = "user_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_template_id")
    private NotificationTemplate template;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> contextData;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 기본값 설정

    @Builder
    public UserNotification(User user, 
                            NotificationTemplate template, 
                            Map<String, Object> contextData,
                            Boolean isRead, 
                            Boolean isDeleted) {
        // 필수 필드 검증
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (contextData == null) {
            throw new IllegalArgumentException("ContextData cannot be null");
        }
        
        this.user = user;
        this.template = template;
        this.contextData = contextData;
        this.isRead = isRead != null ? isRead : false;
        this.isDeleted = isDeleted != null ? isDeleted : false;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

}
