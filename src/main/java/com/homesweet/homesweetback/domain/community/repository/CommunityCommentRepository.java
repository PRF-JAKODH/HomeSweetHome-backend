package com.homesweet.homesweetback.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.homesweet.homesweetback.domain.community.entity.*;
import java.util.List;

/**
 * CommunityComment 레포
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

public interface CommunityCommentRepository extends JpaRepository<CommunityCommentEntity, Long> {

    // 특정 게시글의 댓글 조회
    List<CommunityCommentEntity> findByPost_PostIdAndIsDeletedFalse(Long postId);

}
