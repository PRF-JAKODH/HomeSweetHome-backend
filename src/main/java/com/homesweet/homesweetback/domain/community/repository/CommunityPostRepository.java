package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    // ========== 벌크 업데이트 (데드락 방지) ==========

    /**
     * 조회수 증가 (벌크 업데이트)
     * Entity 조회 없이 UPDATE 쿼리 직접 실행
     */
    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId AND p.isDeleted = false")
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 좋아요 카운트 변경 (벌크 업데이트)
     * @param delta: +1 (증가) 또는 -1 (감소)
     */
    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.likeCount = p.likeCount + :delta WHERE p.postId = :postId AND p.isDeleted = false")
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * 댓글 카운트 변경 (벌크 업데이트)
     * @param delta: +1 (증가) 또는 -1 (감소)
     */
    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.commentCount = p.commentCount + :delta WHERE p.postId = :postId AND p.isDeleted = false")
    int updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);
}