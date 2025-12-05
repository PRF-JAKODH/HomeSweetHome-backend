package com.homesweet.homesweetback.domain.community.dto;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostResponse(
        Long postId,
        Long authorId,
        String authorName,
        String title,
        String content,
        String category,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Boolean isModified,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        List<String> imagesUrl
) {
    public static CommunityPostResponse from(CommunityPostEntity entity, List<String> imageUrls) {
        return new CommunityPostResponse(
                entity.getPostId(),
                entity.getAuthor().getId(),
                entity.getAuthor().getName(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCategory(),
                entity.getViewCount(),
                entity.getLikeCount(),
                entity.getCommentCount(),
                entity.getIsModified(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                (imageUrls.isEmpty()) ? List.of() : imageUrls
        );
    }

    /**
     * Cache-Aside 패턴용: Redis에서 조회한 카운터 값을 사용
     */
    public static CommunityPostResponse fromWithCachedCounts(
            CommunityPostEntity entity,
            List<String> imageUrls,
            Integer viewCount,
            Integer likeCount,
            Integer commentCount
    ) {
        return new CommunityPostResponse(
                entity.getPostId(),
                entity.getAuthor().getId(),
                entity.getAuthor().getName(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCategory(),
                viewCount,
                likeCount,
                commentCount,
                entity.getIsModified(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                (imageUrls == null || imageUrls.isEmpty()) ? List.of() : imageUrls
        );
    }
}