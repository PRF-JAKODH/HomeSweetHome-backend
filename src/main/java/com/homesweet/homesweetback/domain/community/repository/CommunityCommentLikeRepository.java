package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.homesweet.homesweetback.domain.community.entity.*;

import java.util.Optional;

/**
 * CommunityCommentLike 레포
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
public interface CommunityCommentLikeRepository extends JpaRepository<CommunityCommentLikeEntity, Long> {
    Optional<CommunityCommentLikeEntity> findByCommentAndUser(CommunityCommentEntity comment, User user);
    boolean existsByComment_CommentIdAndUser_Id(Long commentId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CommunityCommentLikeEntity cl WHERE cl.comment.commentId = :commentId AND cl.user.id = :userId")
    int deleteByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);
}