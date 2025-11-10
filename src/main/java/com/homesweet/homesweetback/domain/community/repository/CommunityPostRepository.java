package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * CommunityPost 레포
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

public interface CommunityPostRepository extends JpaRepository<CommunityPostEntity, Long> {

    // 특정 게시글 조회
    Optional<CommunityPostEntity> findByPostIdAndIsDeletedFalse(Long postId);

    // 비관적 락을 사용한 게시글 조회 (동시성 제어용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM CommunityPostEntity p WHERE p.postId = :postId AND p.isDeleted = false")
    Optional<CommunityPostEntity> findByPostIdAndIsDeletedFalseWithPessimisticLock(@Param("postId") Long postId);

    // 페이지네이션 쿼리 메서드
    Page<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);
}