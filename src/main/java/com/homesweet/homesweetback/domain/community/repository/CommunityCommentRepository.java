package com.homesweet.homesweetback.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.homesweet.homesweetback.domain.community.entity.*;
import java.util.List;

/**
 * CommunityComment 레포
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

public interface CommunityCommentRepository extends JpaRepository<CommunityCommentEntity, Long> {

    // 특정 게시글의 댓글 조회
    List<CommunityCommentEntity> findByPost_PostIdAndIsDeletedFalse(Long postId);

    // 직접 UPDATE 쿼리 - Native Query로 S-LOCK 획득 없이 바로 X-LOCK
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE community_comments SET like_count = like_count + 1 WHERE comment_id = :commentId", nativeQuery = true)
    int incrementLikeCount(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE community_comments SET like_count = like_count - 1 WHERE comment_id = :commentId", nativeQuery = true)
    int decrementLikeCount(@Param("commentId") Long commentId);
}
