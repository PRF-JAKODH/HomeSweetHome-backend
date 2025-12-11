package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface CommunityPostRepository extends JpaRepository<CommunityPostEntity, Long> {
    // 특정 게시글 조회 - N+1 문제 해결
    @EntityGraph(attributePaths = {"author", "author.grade"})
    Optional<CommunityPostEntity> findByPostIdAndIsDeletedFalse(Long postId);

    // 페이지네이션 쿼리 메서드 / N+1문제 해결위해 EntityGraph 추가
    // Slice 사용 - COUNT 쿼리 자동 실행 방지 (totalCount는 Redis에서 별도 관리)
    @EntityGraph(attributePaths = {"author", "author.grade"})
    Slice<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);

    // 전체 게시글 수 조회 (캐싱용)
    long countByIsDeletedFalse();

    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.viewCount = :viewCount WHERE p.postId = :postId AND p.isDeleted = false")
    int updateViewCount(@Param("postId") Long postId, @Param("viewCount") Integer viewCount);

    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.commentCount = :commentCount WHERE p.postId = :postId AND p.isDeleted = false")
    int setCommentCount(@Param("postId") Long postId, @Param("commentCount") Integer commentCount);

    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.likeCount = :likeCount WHERE p.postId = :postId AND p.isDeleted = false")
    int setLikeCount(@Param("postId") Long postId, @Param("likeCount") Integer likeCount);
}