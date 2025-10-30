package com.homesweet.homesweetback.domain.notification.repository;

import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 알림 리포지토리
 * 
 * @author dogyungkim
 */
@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    
    /**
     * 사용자의 알림 목록 조회 (삭제되지 않은 것만, 최신순)
     */
    Page<UserNotification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 사용자의 알림 목록 조회 (최대 20개, 최신순)
     * template과 category를 함께 조회하여 N+1 문제 방지
     */
    @Query("SELECT DISTINCT un FROM UserNotification un " +
           "JOIN FETCH un.template " +
           "WHERE un.user.id = :userId AND un.isDeleted = false " +
           "ORDER BY un.createdAt DESC")
    List<UserNotification> findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    /**
     * 사용자의 읽지 않은 알림 개수 조회
     */
    long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);
    
    /**
     * 사용자의 특정 알림 조회 (본인 것만)
     */
    @Query("SELECT un FROM UserNotification un WHERE un.id = :notificationId AND un.user.id = :userId AND un.isDeleted = false")
    Optional<UserNotification> findByIdAndUserIdAndNotDeleted(@Param("notificationId") Long notificationId, 
                                                              @Param("userId") Long userId);
    
    /**
     * 사용자의 모든 읽지 않은 알림 조회
     */
    List<UserNotification> findByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);
    
    /**
     * 사용자의 여러 알림 조회 (본인 것만)
     */
    @Query("SELECT un FROM UserNotification un WHERE un.id IN :notificationIds AND un.user.id = :userId AND un.isDeleted = false")
    List<UserNotification> findByIdInAndUserIdAndNotDeleted(@Param("notificationIds") List<Long> notificationIds, 
                                                             @Param("userId") Long userId);
}
