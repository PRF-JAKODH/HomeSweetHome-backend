package com.homesweet.homesweetback.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}