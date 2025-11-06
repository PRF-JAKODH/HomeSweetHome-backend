package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
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
}