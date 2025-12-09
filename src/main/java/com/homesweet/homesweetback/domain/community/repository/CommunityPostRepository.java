package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;

public interface CommunityPostRepository extends JpaRepository<CommunityPostEntity, Long> {
    // 특정 게시글 조회 - N+1 문제 해결
    @EntityGraph(attributePaths = {"author", "author.grade"})
    Optional<CommunityPostEntity> findByPostIdAndIsDeletedFalse(Long postId);

    // 페이지네이션 쿼리 메서드 / N+1문제 해결위해 EntityGraph 추가
    @EntityGraph(attributePaths = {"author", "author.grade"})
    Page<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);

    // 전체 게시글 수 조회 (캐싱용)
    long countByIsDeletedFalse();

    // 캐시 워밍업용: 인기 게시글 상위 100개
    @EntityGraph(attributePaths = {"author", "author.grade"})
    @Query("SELECT p FROM CommunityPostEntity p WHERE p.isDeleted = false ORDER BY p.viewCount DESC LIMIT 100")
    List<CommunityPostEntity> findTop100ByIsDeletedFalseOrderByViewCountDesc();

    // 캐시 워밍업용: 최신 게시글 50개
    @EntityGraph(attributePaths = {"author", "author.grade"})
    @Query("SELECT p FROM CommunityPostEntity p WHERE p.isDeleted = false ORDER BY p.createdAt DESC LIMIT 50")
    List<CommunityPostEntity> findTop50ByIsDeletedFalseOrderByCreatedAtDesc();

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