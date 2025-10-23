package com.homesweet.homesweetback.domain.community.dto;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import java.time.LocalDateTime;

/**
 * Community 응답 DTO
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
public record CommunityResponse(
        Long postId,
        Long authorId,
        String authorName,
        String title,
        String content,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Boolean isModified,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static CommunityResponse from(CommunityPostEntity entity) {
        // TODO: Grade 엔티티 이슈 해결 후 User 연관관계에서 이름 가져오기
        return new CommunityResponse(
                entity.getPostId(),
                entity.getUserId(),  // TODO: entity.getAuthor().getId()로 변경
                "임시사용자",  // TODO: entity.getAuthor().getName()로 변경
                entity.getTitle(),
                entity.getContent(),
                entity.getViewCount(),
                entity.getLikeCount(),
                entity.getCommentCount(),
                entity.getIsModified(),
                entity.getCreatedAt(),
                entity.getModifiedAt()
        );
    }
}
