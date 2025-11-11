package com.homesweet.homesweetback.domain.community.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.homesweet.homesweetback.domain.community.entity.*;
import java.util.List;
import java.util.Optional;

/**
 * CommunityComment 레포
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

public interface CommunityCommentRepository extends JpaRepository<CommunityCommentEntity, Long> {

    // 특정 게시글의 댓글 조회
    List<CommunityCommentEntity> findByPost_PostIdAndIsDeletedFalse(Long postId);

    // 비관적 락을 사용한 댓글 조회 (동시성 제어용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CommunityCommentEntity c WHERE c.commentId = :commentId")
    Optional<CommunityCommentEntity> findByIdWithPessimisticLock(@Param("commentId") Long commentId);

}
