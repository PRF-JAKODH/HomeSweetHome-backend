package com.homesweet.homesweetback.domain.notification.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 카테고리 엔티티
 * 
 * @author dogyungkim
 */
@Entity
@Table(name = "notification_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_category_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false, length = 50)
    private NotificationCategoryType categoryType;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public NotificationCategory(NotificationCategoryType categoryType) {
        this.categoryType = categoryType;
    }
}
