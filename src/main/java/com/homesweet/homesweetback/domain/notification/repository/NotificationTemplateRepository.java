package com.homesweet.homesweetback.domain.notification.repository;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 알림 템플릿 리포지토리
 * 
 * @author dogyungkim
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByTemplateType(NotificationEventType templateType);
}