package com.homesweet.homesweetback.domain.community.dto;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime modifiedAt,
        List<String> imagesUrl
) {
    public static CommunityResponse from(CommunityPostEntity entity, List<String> imageUrls) {
        return new CommunityResponse(
                entity.getPostId(),
                entity.getAuthor().getId(),
                entity.getAuthor().getName(),
                entity.getTitle(),
                entity.getContent(),
                entity.getViewCount(),
                entity.getLikeCount(),
                entity.getCommentCount(),
                entity.getIsModified(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                (imageUrls.isEmpty()) ? List.of() : imageUrls
        );
    }
}