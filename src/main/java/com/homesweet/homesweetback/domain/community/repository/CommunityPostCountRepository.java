package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * CommunityPostCount 레포지토리
 * - Native SQL로 S-LOCK 없이 직접 X-LOCK 획득
 * - 게시글 본문 테이블과 독립적인 락 관리
 *
 * @author ohhalim777@gmail.com
 * @date 25. 11. 24.
 */
public interface CommunityPostCountRepository extends JpaRepository<CommunityPostCount, Long> {

    /**
     * 비관적 락으로 카운트 조회 (데드락 방지용)
     * - 좋아요 토글 시 가장 먼저 이 락을 획득하여 락 순서 통일
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CommunityPostCount c WHERE c.postId = :postId")
    Optional<CommunityPostCount> findByIdWithLock(@Param("postId") Long postId);

    /**
     * 조회수 증가 (Native SQL - 바로 X-LOCK 획득)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE community_post_counts SET view_count = view_count + 1 WHERE post_id = :postId", nativeQuery = true)
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 좋아요 수 증가 (Native SQL - 바로 X-LOCK 획득)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE community_post_counts SET like_count = like_count + 1 WHERE post_id = :postId", nativeQuery = true)
    int incrementLikeCount(@Param("postId") Long postId);

    /**
     * 좋아요 수 감소 (Native SQL - 바로 X-LOCK 획득)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE community_post_counts SET like_count = like_count - 1 WHERE post_id = :postId", nativeQuery = true)
    int decrementLikeCount(@Param("postId") Long postId);

    /**
     * 댓글 수 증가 (Native SQL - 바로 X-LOCK 획득)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE community_post_counts SET comment_count = comment_count + 1 WHERE post_id = :postId", nativeQuery = true)
    int incrementCommentCount(@Param("postId") Long postId);

    /**
     * 댓글 수 감소 (Native SQL - 바로 X-LOCK 획득)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE community_post_counts SET comment_count = comment_count - 1 WHERE post_id = :postId", nativeQuery = true)
    int decrementCommentCount(@Param("postId") Long postId);
}
