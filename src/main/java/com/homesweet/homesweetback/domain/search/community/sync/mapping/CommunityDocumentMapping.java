package com.homesweet.homesweetback.domain.search.community.sync.mapping;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.search.community.repository.document.CommunityPostDocument;
import org.springframework.stereotype.Component;

/**
 * 게시글 도큐먼트 엔티티 매핑
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Component
public class CommunityDocumentMapping {

    public CommunityPostDocument convert(CommunityPostEntity post) {

        return CommunityPostDocument.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .authorId(post.getAuthor().getId())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isDeleted(post.getIsDeleted())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
