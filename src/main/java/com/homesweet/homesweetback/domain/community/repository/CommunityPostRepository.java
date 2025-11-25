package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // 페이지네이션 쿼리 메서드
    Page<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CommunityPostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId AND p.isDeleted = false")
    int incrementViewCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CommunityPostEntity p SET p.likeCount = p.likeCount + :delta WHERE p.postId = :postId AND p.isDeleted = false")
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CommunityPostEntity p SET p.commentCount = p.commentCount + :delta WHERE p.postId = :postId AND p.isDeleted = false")
    int updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);
}