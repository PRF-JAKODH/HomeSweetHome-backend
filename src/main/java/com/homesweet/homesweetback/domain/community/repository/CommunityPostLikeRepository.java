package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.homesweet.homesweetback.domain.community.entity.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
}