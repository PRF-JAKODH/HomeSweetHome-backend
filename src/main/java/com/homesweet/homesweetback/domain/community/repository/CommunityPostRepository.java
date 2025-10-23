package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * CommunityPost 레포
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

public interface CommunityPostRepository extends JpaRepository<CommunityPostEntity, Long> {

    // 삭제되지 않은 모든 게시글 조회
    List<CommunityPostEntity> findByIsDeletedFalse();

    // 특정 게시글 조회
    Optional<CommunityPostEntity> findByPostIdAndIsDeletedFalse(Long postId);

    // 특정 사용자의 게시글 조회
    // TODO: Grade 이슈 해결 후 Author_Id로 변경
    List<CommunityPostEntity> findByUserIdAndIsDeletedFalse(Long userId);

    // 제목으로 검색
    List<CommunityPostEntity> findByTitleContainingAndIsDeletedFalse(String keyword);

    // 내용으로 검색
    List<CommunityPostEntity> findByContentContainingAndIsDeletedFalse(String keyword);

    // 제목 또는 내용으로 검색
    @Query("SELECT p FROM CommunityPostEntity p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) AND p.isDeleted = false")
    List<CommunityPostEntity> searchByKeyword(@Param("keyword") String keyword);

    // 인기 게시글 조회 (조회수 기준)
    List<CommunityPostEntity> findByIsDeletedFalseOrderByViewCountDesc();

    // 최신 게시글 조회
    List<CommunityPostEntity> findByIsDeletedFalseOrderByCreatedAtDesc();

    // 특정 사용자의 게시글 개수
    // TODO: Grade 이슈 해결 후 Author_Id로 변경
    Long countByUserIdAndIsDeletedFalse(Long userId);
}