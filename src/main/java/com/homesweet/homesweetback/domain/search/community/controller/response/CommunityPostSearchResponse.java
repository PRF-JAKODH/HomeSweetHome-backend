package com.homesweet.homesweetback.domain.search.community.controller.response;

import com.homesweet.homesweetback.domain.search.community.repository.document.CommunityPostDocument;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CommunityPostSearchResponse(
        Long postId,
        String title,
        String snippet,
        String category,
        Long authorId,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        LocalDateTime createdAt
) {

    /**
     * Document → DTO 변환
     */
    public static CommunityPostSearchResponse from(CommunityPostDocument doc) {

        return CommunityPostSearchResponse.builder()
                .postId(doc.getPostId())
                .title(doc.getTitle())
                .snippet(doc.getContent())
                .category(doc.getCategory())
                .authorId(doc.getAuthorId())
                .viewCount(doc.getViewCount())
                .likeCount(doc.getLikeCount())
                .commentCount(doc.getCommentCount())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}