package com.homesweet.homesweetback.domain.notification.repository;

import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.entity.NotificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 알림 카테고리 리포지토리
 * 
 * @author dogyungkim
 */
@Repository
public interface NotificationCategoryRepository extends JpaRepository<NotificationCategory, Long> {
    Optional<NotificationCategory> findByCategoryType(NotificationCategoryType categoryType);
}