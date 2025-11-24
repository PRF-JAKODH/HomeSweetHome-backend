package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.homesweet.homesweetback.domain.community.entity.*;

import java.util.Optional;

/**
 * CommunityPostLike 레포
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLikeEntity, Long> {
    Optional<CommunityPostLikeEntity> findByPostAndUser(CommunityPostEntity post, User user);
    boolean existsByPost_PostIdAndUser_Id(Long postId, Long userId);

    /**
     * 원자적 DELETE (S-LOCK 없이 바로 X-LOCK 획득)
     */
    @Modifying
    @Query("DELETE FROM CommunityPostLikeEntity pl WHERE pl.post.postId = :postId AND pl.user.id = :userId")
    int deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}