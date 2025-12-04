package com.homesweet.homesweetback.domain.community.dto;

import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityCommentResponse(
        Long commentId,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        Long parentCommentId,
        Integer likeCount,
        Boolean isModified,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static CommunityCommentResponse from(CommunityCommentEntity entity) {
        return new CommunityCommentResponse(
                entity.getCommentId(),
                entity.getPost().getPostId(),
                entity.getAuthor().getId(),
                entity.getAuthor().getName(),
                entity.getContent(),
                entity.getParentCommentId(),
                entity.getLikeCount(),
                entity.getIsModified(),
                entity.getCreatedAt(),
                entity.getModifiedAt()
        );
    }

    /**
     * Cache-Aside 패턴용: Redis에서 조회한 좋아요 수를 사용
     */
    public static CommunityCommentResponse fromWithCachedLikeCount(
            CommunityCommentEntity entity,
            Integer likeCount
    ) {
        return new CommunityCommentResponse(
                entity.getCommentId(),
                entity.getPost().getPostId(),
                entity.getAuthor().getId(),
                entity.getAuthor().getName(),
                entity.getContent(),
                entity.getParentCommentId(),
                likeCount,
                entity.getIsModified(),
                entity.getCreatedAt(),
                entity.getModifiedAt()
        );
    }
}