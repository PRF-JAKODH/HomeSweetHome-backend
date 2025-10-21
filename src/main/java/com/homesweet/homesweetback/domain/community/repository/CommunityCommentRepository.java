package com.homesweet.homesweetback.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.homesweet.homesweetback.domain.community.entity.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * CommunityComment 레포
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

public interface CommunityCommentRepository extends JpaRepository<CommunityCommentEntity, Long> {

    // 삭제되지 않은 모든 게시글 조회
    List<CommunityCommentEntity> findByIsDeletedFalse();

    // 특정 게시글 조회
    List<CommunityCommentEntity> findByPostIdAndIsDeletedFalse(Long postId);

    // 특정 사용자의 게시글 조회
    List<CommunityCommentEntity> findByUserIdAndIsDeletedFalse(Long userId);

    // 내용으로 검색
    List<CommunityCommentEntity> findByContentContainingAndIsDeletedFalse(String keyword);
}
